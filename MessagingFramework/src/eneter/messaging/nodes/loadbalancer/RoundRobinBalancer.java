/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.loadbalancer;

import java.util.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.linq.internal.EnumerableExt;

class RoundRobinBalancer extends AttachableDuplexInputChannelBase
                         implements ILoadBalancer
{
    // Represents a service receiving requests from the load balancer.
    private static class TReceiver
    {
        // Represents a client sending requests via the load balancer.
        public static class TConnection
        {
            public TConnection(String responseReceiverId, IDuplexOutputChannel duplexOutputChannel)
            {
                myResponseReceiverId = responseReceiverId;
                myDuplexOutputChannel = duplexOutputChannel;
            }
            
            public String getResponseReceiverId()
            {
                return myResponseReceiverId;
            }
            
            public IDuplexOutputChannel getDuplexOutputChannel()
            {
                return myDuplexOutputChannel;
            }

            public String myResponseReceiverId;
            public IDuplexOutputChannel myDuplexOutputChannel;
        }

        public TReceiver(String duplexOutputChannelId)
        {
            myChannelId = duplexOutputChannelId;
        }

        public String getChannelId()
        {
            return myChannelId;
        }
        
        public HashSet<TConnection> getOpenConnections()
        {
            return myOpenConnections;
        }

        private String myChannelId;
        private HashSet<TConnection> myOpenConnections = new HashSet<TConnection>();
    }
    

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEventImpl.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEventImpl.getApi();
    }

    
    public RoundRobinBalancer(IMessagingSystemFactory outputMessagingFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myOutputMessagingFactory = outputMessagingFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public void addDuplexOutputChannel(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myAvailableReceivers)
            {
                TReceiver aReceiver = new TReceiver(channelId);
                myAvailableReceivers.add(aReceiver);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void removeDuplexOutputChannel(final String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myAvailableReceivers)
            {
                // Find the receiver with the given channel id.
                TReceiver aReceiver = EnumerableExt.firstOrDefault(myAvailableReceivers,
                        new IFunction1<Boolean, TReceiver>()
                        {
                            @Override
                            public Boolean invoke(TReceiver x) throws Exception
                            {
                                return x.getChannelId().equals(channelId);
                            }
                        });
                        
                if (aReceiver != null)
                {
                    // Try to close all open duplex output channels.
                    for (TReceiver.TConnection aConnection : aReceiver.getOpenConnections())
                    {
                        try
                        {
                            aConnection.getDuplexOutputChannel().closeConnection();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to close connection to " + channelId, err);
                        }

                        aConnection.getDuplexOutputChannel().responseMessageReceived().unsubscribe(myOnResponseMessageReceivedHandler);

                        // Note: The client (response receiver) cannot be disconnected because it can have connections
                        //       with multiple services (receivers) from the pool.
                    }

                    // Remove the connection from available connections.
                    myAvailableReceivers.remove(aReceiver);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void removeAllDuplexOutputChannels()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myAvailableReceivers)
            {
                // Go via all available receivers.
                for (TReceiver aReceiver : myAvailableReceivers)
                {
                    // Try to close all open duplex output channels.
                    for (TReceiver.TConnection aConnection : aReceiver.getOpenConnections())
                    {
                        try
                        {
                            aConnection.getDuplexOutputChannel().closeConnection();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to close connection to " + aConnection.getDuplexOutputChannel().getChannelId(), err);
                        }

                        aConnection.getDuplexOutputChannel().responseMessageReceived().unsubscribe(myOnResponseMessageReceivedHandler);

                        // Note: The client (response receiver) cannot be disconnected because even if all services (receivers)
                        //       are removed from the pool, lated on can be added the new one which can process requests.
                    }
                }

                // Clean available receivers.
                myAvailableReceivers.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Receives a message from the client and fords it to the first available service.
     */
    @Override
    protected void onRequestMessageReceived(Object sender, final DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myAvailableReceivers)
            {
                if (myAvailableReceivers.size() == 0)
                {
                    EneterTrace.warning(TracedObject() + " could not forward the request because there are no attached duplex output channels.");
                    return;
                }

                // Try to forward the incoming message to the first available receiver.
                for (int i = 0; i < myAvailableReceivers.size(); ++i)
                {
                    TReceiver aReceiver = myAvailableReceivers.get(i);

                    // If there is not open connection for the current response receiver id, then open it.
                    TReceiver.TConnection aConnection = null; 
                    try
                    {
                        aConnection = EnumerableExt.firstOrDefault(aReceiver.getOpenConnections(),
                                new IFunction1<Boolean, TReceiver.TConnection>()
                                {
                                    @Override
                                    public Boolean invoke(TReceiver.TConnection x)
                                            throws Exception
                                    {
                                        return x.getResponseReceiverId().equals(e.getResponseReceiverId());
                                    }
                                });
                    }
                    catch (Exception err)
                    {
                        // If we are here then there is a programatical error in the invoke() method.
                        EneterTrace.error(TracedObject() + "failed during EnumerableExt.firstOrDefault().", err);
                    }
                    if (aConnection == null)
                    {
                        try
                        {
                            IDuplexOutputChannel anOutputChannel = myOutputMessagingFactory.createDuplexOutputChannel(aReceiver.getChannelId());
                            aConnection = new TReceiver.TConnection(e.getResponseReceiverId(), anOutputChannel);
                            
                            aConnection.getDuplexOutputChannel().responseMessageReceived().subscribe(myOnResponseMessageReceivedHandler);
                            aConnection.getDuplexOutputChannel().openConnection();

                            aReceiver.getOpenConnections().add(aConnection);
                        }
                        catch (Exception err)
                        {
                            aConnection.getDuplexOutputChannel().responseMessageReceived().unsubscribe(myOnResponseMessageReceivedHandler);
                            EneterTrace.warning(TracedObject() + ErrorHandler.OpenConnectionFailure, err);
                            
                            // Try to forward the request to the next receiver.
                            continue;
                        }
                    }

                    // Forward the message to the "service".
                    try
                    {
                        aConnection.getDuplexOutputChannel().sendMessage(e.getMessage());
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.SendMessageFailure, err);

                        // Remove this unavailable connection from connections of this receiver.
                        try
                        {
                            aConnection.getDuplexOutputChannel().closeConnection();
                        }
                        catch (Exception err2)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err2);
                        }
                        aConnection.getDuplexOutputChannel().responseMessageReceived().unsubscribe(myOnResponseMessageReceivedHandler);
                        aReceiver.getOpenConnections().remove(aConnection);
                        
                        // Try to forward the request to the next receiver.
                        continue;
                    }

                    // Put the used receiver to the end.
                    myAvailableReceivers.remove(i);
                    myAvailableReceivers.add(aReceiver);

                    // The sending was successful.
                    break;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseMessageReceived(Object sender, final DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aResponseReceiverId = null;

            synchronized (myAvailableReceivers)
            {
                // The response receiver id coming with the message belongs to the duplex output channel.
                // So we need to find the response receiver id for the duplex input channel.
                TReceiver aReceiver = null;
                try
                {
                    aReceiver = EnumerableExt.firstOrDefault(myAvailableReceivers,
                            new IFunction1<Boolean, TReceiver>()
                            {
                                @Override
                                public Boolean invoke(TReceiver x) throws Exception
                                {
                                    return x.getChannelId().equals(e.getChannelId());
                                }
                            });
                }
                catch (Exception err)
                {
                    // If we are here then there is a programatical error in the invoke() method.
                    EneterTrace.error(TracedObject() + "failed during EnumerableExt.firstOrDefault().", err);
                }

                if (aReceiver != null)
                {
                    TReceiver.TConnection aConnection = null;
                    
                    try
                    {
                        aConnection = EnumerableExt.firstOrDefault(aReceiver.getOpenConnections(),
                                new IFunction1<Boolean, TReceiver.TConnection>()
                                {
                                    @Override
                                    public Boolean invoke(TReceiver.TConnection x)
                                            throws Exception
                                    {
                                        return x.getDuplexOutputChannel().getResponseReceiverId().equals(e.getResponseReceiverId());
                                    }
                                });
                    }
                    catch(Exception err)
                    {
                     // If we are here then there is a programatical error in the invoke() method.
                        EneterTrace.error(TracedObject() + "failed during EnumerableExt.firstOrDefault().", err);
                    }
                            
                    if (aConnection != null)
                    {
                        aResponseReceiverId = aConnection.getResponseReceiverId();
                    }
                }
            }

            if (aResponseReceiverId == null)
            {
                EneterTrace.warning(TracedObject() + "could not find receiver for the incoming response message.");
                return;
            }

            synchronized (myDuplexInputChannelManipulatorLock)
            {
                IDuplexInputChannel aDuplexInputChannel = getAttachedDuplexInputChannel(); 
                // Send the response message via the duplex input channel to the sender.
                if (aDuplexInputChannel != null)
                {
                    try
                    {
                        aDuplexInputChannel.sendResponseMessage(aResponseReceiverId, e.getMessage());
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
                    }
                }
                else
                {
                    EneterTrace.error(TracedObject() + "cannot send the response message when the duplex input channel is not attached.");
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverConnectedEventImpl.isSubscribed())
            {
                try
                {
                    myResponseReceiverConnectedEventImpl.raise(this, e);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void onResponseReceiverDisconnected(Object sender, final ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myAvailableReceivers)
            {
                // Go via all available receivers and close all open channels for the disconnecting response receiver.
                for (TReceiver aReceiver : myAvailableReceivers)
                {
                    TReceiver.TConnection aConnection = null;
                    try
                    {
                        // Try to close all the open duplex output channel for the disconnecting response receiver
                        aConnection = EnumerableExt.firstOrDefault(aReceiver.getOpenConnections(),
                                new IFunction1<Boolean, TReceiver.TConnection>()
                                {
                                    @Override
                                    public Boolean invoke(TReceiver.TConnection x) throws Exception
                                    {
                                        return x.getResponseReceiverId().equals(e.getResponseReceiverId());
                                    }
                                });
                    }
                    catch (Exception err)
                    {
                        // If we are here then this is a programatical error in the invoke() method.
                        EneterTrace.error(TracedObject() + "failed during EnumerableExt.firstOrDefault()", err);
                    }
                            
                    if (aConnection != null)
                    {
                        try
                        {
                            aConnection.getDuplexOutputChannel().closeConnection();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to close connection to " + aConnection.getDuplexOutputChannel().getChannelId(), err);
                        }

                        aConnection.getDuplexOutputChannel().responseMessageReceived().unsubscribe(myOnResponseMessageReceivedHandler);

                        aReceiver.getOpenConnections().remove(aConnection);
                    }
                }

                if (myResponseReceiverDisconnectedEventImpl.isSubscribed())
                {
                    try
                    {
                        myResponseReceiverDisconnectedEventImpl.raise(this, e);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    
    
    private IMessagingSystemFactory myOutputMessagingFactory;
    private ArrayList<TReceiver> myAvailableReceivers = new ArrayList<TReceiver>();

    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnResponseMessageReceivedHandler = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onResponseMessageReceived(sender, e);
        }
    };
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }

}

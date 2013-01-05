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

    @Override
    public void addDuplexOutputChannel(String channelId)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeDuplexOutputChannel(String channelId)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeAllDuplexOutputChannels()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        // TODO Auto-generated method stub
        
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
        return "RoundRobinBalancer ";
    }

}

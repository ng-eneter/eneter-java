/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

import java.util.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.infrastructure.attachable.*;
import eneter.messaging.infrastructure.attachable.internal.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.linq.internal.EnumerableExt;


class DuplexDispatcher implements IAttachableMultipleDuplexInputChannels, IDuplexDispatcher
{
    // Represents one particular client which is connected via the input channel.
    private class TClient
    {
        public TClient(IDuplexInputChannel inputChannel, String inputResponseReceiverId)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myInputChannel = inputChannel;
                myInputResponseReceiverId = inputResponseReceiverId;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        // Client opens connections to all available outputs.
        public void openOutputConnections(IMessagingSystemFactory messaging, Iterable<String> availableOutputChannelIds)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                for (String aChannelId : availableOutputChannelIds)
                {
                    openOutputConnection(messaging, aChannelId);
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        // Client opens connection to a particular output.
        public void openOutputConnection(IMessagingSystemFactory messaging, String channelId)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                IDuplexOutputChannel anOutputChannel = null;
                try
                {
                    myOutputConnectionLock.lock();
                    try
                    {
                        anOutputChannel = messaging.createDuplexOutputChannel(channelId);
                        anOutputChannel.connectionClosed().subscribe(myOnConnectionClosed);
                        anOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);

                        anOutputChannel.openConnection();

                        // Connection is successfully open so it can be stored.
                        myOpenOutputConnections.add(anOutputChannel);
                    }
                    finally
                    {
                        myOutputConnectionLock.unlock();
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.warning("Failed to open connection to '" + channelId + "'.", err);

                    if (anOutputChannel != null)
                    {
                        anOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosed);
                        anOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);
                    }
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        // Client closes a particular connection.
        public void closeOutputConnection(String channelId)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myOutputConnectionLock.lock();
                try
                {
                    for (int i = myOpenOutputConnections.size() - 1; i >= 0; --i)
                    {
                        if (myOpenOutputConnections.get(i).getChannelId().equals(channelId))
                        {
                            myOpenOutputConnections.get(i).closeConnection();

                            myOpenOutputConnections.get(i).connectionClosed().unsubscribe(myOnConnectionClosed);
                            myOpenOutputConnections.get(i).responseMessageReceived().unsubscribe(myOnResponseMessageReceived);

                            myOpenOutputConnections.remove(i);
                        }
                    }
                }
                finally
                {
                    myOutputConnectionLock.unlock();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        // Client closes connections to all output.
        public void closeOutpuConnections()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myOutputConnectionLock.lock();
                try
                {
                    for (IDuplexOutputChannel anOutputChannel : myOpenOutputConnections)
                    {
                        anOutputChannel.closeConnection();

                        anOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosed);
                        anOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);
                    }
                    myOpenOutputConnections.clear();
                }
                finally
                {
                    myOutputConnectionLock.unlock();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        // Client forwards the message to all output connections.
        public void forwardMessage(Object message)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                IDuplexOutputChannel[] anOutputChannels = null;

                myOutputConnectionLock.lock();
                try
                {
                    anOutputChannels = new IDuplexOutputChannel[myOpenOutputConnections.size()];
                    anOutputChannels = myOpenOutputConnections.toArray(anOutputChannels);
                }
                finally
                {
                    myOutputConnectionLock.unlock();
                }

                // Forward the incoming message to all output channels.
                for (IDuplexOutputChannel anOutputChannel : anOutputChannels)
                {
                    try
                    {
                        anOutputChannel.sendMessage(message);
                    }
                    catch (Exception err)
                    {
                        // Note: do not rethrow the exception because it woiuld stop forwarding the message to other output channels.
                        EneterTrace.warning("Failed to send message to '" + anOutputChannel.getChannelId() + "'.", err);
                    }
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        public boolean isAssociatedResponseReceiverId(final String responseReceiverId)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                boolean isAny = false;
                try
                {
                    myOutputConnectionLock.lock();
                    try
                    {
                        isAny = EnumerableExt.any(myOpenOutputConnections, new IFunction1<Boolean, IDuplexOutputChannel>()
                        {
                            @Override
                            public Boolean invoke(IDuplexOutputChannel x)
                                    throws Exception
                            {
                                return x.getResponseReceiverId().equals(responseReceiverId);
                            }
                        });
                    }
                    finally
                    {
                        myOutputConnectionLock.unlock();
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.error("Failed to check if response receiver is associated.", err);
                }
                
                return isAny;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        // When some output connection was closed/broken.
        private void onConnectionClosed(Object sender, DuplexChannelEventArgs e)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myOutputConnectionLock.lock();
                try
                {
                    IDuplexOutputChannel anOutputChannel = (IDuplexOutputChannel)sender;
                    anOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosed);
                    anOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceived);

                    myOpenOutputConnections.remove(anOutputChannel);
                }
                finally
                {
                    myOutputConnectionLock.unlock();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        // When client received a message from an output connection.
        private void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                try
                {
                    myInputChannel.sendResponseMessage(myInputResponseReceiverId, e.getMessage());
                }
                catch (Exception err)
                {
                    EneterTrace.warning("Failed to send message via the input channel '" + myInputChannel.getChannelId() + "'.", err);
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private String myInputResponseReceiverId;
        private ThreadLock myOutputConnectionLock = new ThreadLock();
        private ArrayList<IDuplexOutputChannel> myOpenOutputConnections = new ArrayList<IDuplexOutputChannel>();
        private IDuplexInputChannel myInputChannel;
        
        private EventHandler<DuplexChannelEventArgs> myOnConnectionClosed = new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object sender, DuplexChannelEventArgs e)
            {
                onConnectionClosed(sender, e);
            }
        };
        
        private EventHandler<DuplexChannelMessageEventArgs> myOnResponseMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
            {
                onResponseMessageReceived(sender, e);
            }
        };
    }
    
    
    // Maintains one particulat input channel and all its connected clients.
    private class TInputChannelContext extends AttachableDuplexInputChannelBase implements IAttachableDuplexInputChannel
    {
        public TInputChannelContext(IMessagingSystemFactory messaging, IFunction<Iterable<String>> getOutputChannelIds)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myMessaging = messaging;
                myGetOutputChannelIds = getOutputChannelIds;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        @Override
        public void detachDuplexInputChannel()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                super.detachDuplexInputChannel();

                myClientConnectionLock.lock();
                try
                {
                    // Close connections of all clients.
                    for (Map.Entry<String, TClient> aClient : myConnectedClients.entrySet())
                    {
                        aClient.getValue().closeOutpuConnections();
                    }
                    myConnectedClients.clear();
                }
                finally
                {
                    myClientConnectionLock.unlock();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        // Goes via all connected clients and opens the new output connection.
        public void openConnection(String channelId)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientConnectionLock.lock();
                try
                {
                    for (Map.Entry<String, TClient> aClient : myConnectedClients.entrySet())
                    {
                        aClient.getValue().openOutputConnection(myMessaging, channelId);
                    }
                }
                finally
                {
                    myClientConnectionLock.unlock();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        // Goes via all connected clients and closes one particular output connection.
        public void closeConnection(String channelId)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientConnectionLock.lock();
                try
                {
                    for (Map.Entry<String, TClient> aClient : myConnectedClients.entrySet())
                    {
                        aClient.getValue().closeOutputConnection(channelId);
                    }
                }
                finally
                {
                    myClientConnectionLock.unlock();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        public String getAssociatedResponseReceiverId(String responseReceiverId)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientConnectionLock.lock();
                try
                {
                    for (Map.Entry<String, TClient> aClient : myConnectedClients.entrySet())
                    {
                        if (aClient.getValue().isAssociatedResponseReceiverId(responseReceiverId))
                        {
                            return aClient.getKey();
                        }
                    }

                    return null;
                }
                finally
                {
                    myClientConnectionLock.unlock();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        

        @Override
        protected void onRequestMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                TClient aClient;
                myClientConnectionLock.lock();
                try
                {
                    aClient = myConnectedClients.get(e.getResponseReceiverId());
                }
                finally
                {
                    myClientConnectionLock.unlock();
                }

                if (aClient != null)
                {
                    aClient.forwardMessage(e.getMessage());
                }
                else
                {
                    EneterTrace.warning(TracedObject() + "failed to forward the message because ResponseReceiverId '" + e.getResponseReceiverId() + "' was not found among open connections.");
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
                TClient aNewClient = new TClient(getAttachedDuplexInputChannel(), e.getResponseReceiverId());
                Iterable<String> anOutputChannelIds = myGetOutputChannelIds.invoke();

                myClientConnectionLock.lock();
                try
                {
                    // Opens connections to all available outputs.
                    aNewClient.openOutputConnections(myMessaging, anOutputChannelIds);

                    myConnectedClients.put(e.getResponseReceiverId(), aNewClient);
                }
                finally
                {
                    myClientConnectionLock.unlock();
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to handle response receiver connected.", err);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        protected void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientConnectionLock.lock();
                try
                {
                    TClient aClient = myConnectedClients.get(e.getResponseReceiverId());
                    if (aClient != null)
                    {
                        aClient.closeOutpuConnections();
                        myConnectedClients.remove(e.getResponseReceiverId());
                    }
                }
                finally
                {
                    myClientConnectionLock.unlock();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        
        private ThreadLock myClientConnectionLock = new ThreadLock();
        private HashMap<String, TClient> myConnectedClients = new HashMap<String, TClient>();
        private IMessagingSystemFactory myMessaging;
        private IFunction<Iterable<String>> myGetOutputChannelIds;
        
        @Override
        protected String TracedObject()
        {
            return getClass().getSimpleName() + ' ';
        }
    
    }
    
    
    
    public DuplexDispatcher(IMessagingSystemFactory duplexOutputChannelMessagingSystem)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessagingSystemFactory = duplexOutputChannelMessagingSystem;
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
            myChannelManipulatorLock.lock();
            try
            {
                myOutputChannelIds.add(channelId);

                // All clients open the new output connection to added channel id.
                for (TInputChannelContext anInputChannelContext : myInputChannelContexts)
                {
                    anInputChannelContext.openConnection(channelId);
                }
            }
            finally
            {
                myChannelManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void removeDuplexOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelManipulatorLock.lock();
            try
            {
                myOutputChannelIds.remove(channelId);
                closeOutputChannel(channelId);
            }
            finally
            {
                myChannelManipulatorLock.unlock();
            }
            
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void removeAllDuplexOutputChannels() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelManipulatorLock.lock();
            try
            {
                try
                {
                    for (String aDuplexOutputChannelId : myOutputChannelIds)
                    {
                        closeOutputChannel(aDuplexOutputChannelId);
                    }
                }
                finally
                {
                    myOutputChannelIds.clear();
                }
            }
            finally
            {
                myChannelManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    

    @Override
    public void attachDuplexInputChannel(IDuplexInputChannel duplexInputChannel)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelManipulatorLock.lock();
            try
            {
                TInputChannelContext anInpuChannelContext = new TInputChannelContext(myMessagingSystemFactory, myGetOutputChannelIds);
                try
                {
                    myInputChannelContexts.add(anInpuChannelContext);
                    anInpuChannelContext.attachDuplexInputChannel(duplexInputChannel);
                }
                catch (Exception err)
                {
                    myInputChannelContexts.remove(anInpuChannelContext);
                    EneterTrace.error(TracedObject() + "failed to attach duplex input channel.", err);
                }
            }
            finally
            {
                myChannelManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void detachDuplexInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelManipulatorLock.lock();
            try
            {
                for (int i = myInputChannelContexts.size() - 1; i >= 0; --i)
                {
                    if (myInputChannelContexts.get(i).getAttachedDuplexInputChannel().getChannelId().equals(channelId))
                    {
                        myInputChannelContexts.get(i).detachDuplexInputChannel();
                        myInputChannelContexts.remove(i);
                        break;
                    }
                }
            }
            finally
            {
                myChannelManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void detachDuplexInputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelManipulatorLock.lock();
            try
            {
                for (TInputChannelContext anInputChannelContext : myInputChannelContexts)
                {
                    anInputChannelContext.detachDuplexInputChannel();
                }

                myInputChannelContexts.clear();
            }
            finally
            {
                myChannelManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public boolean isDuplexInputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelManipulatorLock.lock();
            try
            {
                return myInputChannelContexts.size() > 0;
            }
            finally
            {
                myChannelManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    @Override
    public Iterable<IDuplexInputChannel> getAttachedDuplexInputChannels()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelManipulatorLock.lock();
            try
            {
                ArrayList<IDuplexInputChannel> anInputChannels = new ArrayList<IDuplexInputChannel>();

                for (TInputChannelContext anInputChannelContext : myInputChannelContexts)
                {
                    anInputChannels.add(anInputChannelContext.getAttachedDuplexInputChannel());
                }

                return anInputChannels;
            }
            finally
            {
                myChannelManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public String getAssociatedResponseReceiverId(String responseReceiverId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelManipulatorLock.lock();
            try
            {
                for (TInputChannelContext anInputChannelContext : myInputChannelContexts)
                {
                    String aClientResponseReceiverId = anInputChannelContext.getAssociatedResponseReceiverId(responseReceiverId);
                    if (aClientResponseReceiverId != null)
                    {
                        return aClientResponseReceiverId;
                    }
                }

                return null;
            }
            finally
            {
                myChannelManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void closeOutputChannel(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            for (TInputChannelContext anInputChannelContext : myInputChannelContexts)
            {
                anInputChannelContext.closeConnection(channelId);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private Iterable<String> getOutputChannelIds()
    {
        myChannelManipulatorLock.lock();
        try
        {
            ArrayList<String> aResult = new ArrayList<String>(myOutputChannelIds); 
            return aResult;
        }
        finally
        {
            myChannelManipulatorLock.unlock();
        }
    }
    
    
    private ThreadLock myChannelManipulatorLock = new ThreadLock();
    private IMessagingSystemFactory myMessagingSystemFactory;
    private HashSet<String> myOutputChannelIds = new HashSet<String>();
    private ArrayList<TInputChannelContext> myInputChannelContexts = new ArrayList<TInputChannelContext>();

    
    private IFunction<Iterable<String>> myGetOutputChannelIds = new IFunction<Iterable<String>>()
    {
        @Override
        public Iterable<String> invoke() throws Exception
        {
            return getOutputChannelIds();
        }
    };
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

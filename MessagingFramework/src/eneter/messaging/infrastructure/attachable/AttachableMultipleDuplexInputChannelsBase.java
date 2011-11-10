package eneter.messaging.infrastructure.attachable;

import java.util.ArrayList;
import java.util.HashSet;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.*;
import eneter.net.system.linq.EnumerableExt;

public abstract class AttachableMultipleDuplexInputChannelsBase implements IAttachableMultipleDuplexInputChannels
{
    /// <summary>
    /// Represents the connection between the duplex input channel and the duplex output channel.
    /// So when the response from the duplex output channel is received it can be forwarded to attached the
    /// duplex input channel with the correct response receiver id.
    /// </summary>
    private class TConnection
    {
        public TConnection(String responseReceiverId, IDuplexOutputChannel duplexOutputChannel)
        {
            myResponseReceiverId = responseReceiverId;
            myConnectedDuplexOutputChannel = duplexOutputChannel;
        }

        public String getResponseReceiverId()
        {
            return myResponseReceiverId;
        }
        
        public IDuplexOutputChannel getConnectedDuplexOutputChannel()
        {
            return myConnectedDuplexOutputChannel;
        }
        
        private String myResponseReceiverId;
        private IDuplexOutputChannel myConnectedDuplexOutputChannel;
    }


    /// <summary>
    /// The context of the duplex input channel consists of the attached duplex input channel and
    /// it also can contain the list of duplex output channels used to forward the message.
    /// E.g. The DuplexDispatcher receives the message from the attached duplex input channel and then forwards
    /// it to all duplex output channels.
    /// E.g. The DuplexChannelWrapper receives the message from the attached duplex input channel then wrapps
    /// the message and sends it via the duplex output channel.
    /// </summary>
    private class TDuplexInputChannelContext
    {
        public TDuplexInputChannelContext(IDuplexInputChannel attachedDuplexInputChannel)
        {
            myOpenConnections = new HashSet<TConnection>();

            myAttachedDuplexInputChannel = attachedDuplexInputChannel;
        }

        public IDuplexInputChannel getAttachedDuplexInputChannel()
        {
            return myAttachedDuplexInputChannel;
        }
        public HashSet<TConnection> getOpenConnections()
        {
            return myOpenConnections;
        }
        
        private IDuplexInputChannel myAttachedDuplexInputChannel;
        private HashSet<TConnection> myOpenConnections;
    }

    public void attachDuplexInputChannel(final IDuplexInputChannel duplexInputChannel)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                attach(duplexInputChannel);

                try
                {
                    duplexInputChannel.startListening();
                }
                catch (Exception err)
                {
                    duplexInputChannel.responseReceiverDisconnected().unsubscribe(myResponseReceiverDisconnected);
                    duplexInputChannel.messageReceived().unsubscribe(myMessageReceivedHandler);
                    
                    HashSetExt.removeWhere(myDuplexInputChannelContexts,
                            new IFunction1<Boolean, TDuplexInputChannelContext>()
                            {
                                @Override
                                public Boolean invoke(
                                        TDuplexInputChannelContext x)
                                        throws Exception
                                {
                                    return x.getAttachedDuplexInputChannel().getChannelId() == duplexInputChannel.getChannelId();
                                }
                            });

                    EneterTrace.error(TracedObject() + "failed to attach the duplex input channel '" + duplexInputChannel.getChannelId() + "'.", err);
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void detachDuplexInputChannel(final String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                // Get the context of the requested input channel.
                final TDuplexInputChannelContext aDuplexInputChannelContext = EnumerableExt.firstOrDefault(myDuplexInputChannelContexts,
                        new IFunction1<Boolean, TDuplexInputChannelContext>()
                        {
                            @Override
                            public Boolean invoke(TDuplexInputChannelContext x)
                                    throws Exception
                            {
                                return x.getAttachedDuplexInputChannel().getChannelId() == channelId;
                            }
                        });
                        
                if (aDuplexInputChannelContext != null)
                {
                    try
                    {
                        // Go via all connections with clients and close them.
                        closeConnections(aDuplexInputChannelContext.getOpenConnections());

                        // Stop listening to the duplex input channel.
                        aDuplexInputChannelContext.getAttachedDuplexInputChannel().stopListening();
                    }
                    finally
                    {
                        aDuplexInputChannelContext.getAttachedDuplexInputChannel().responseReceiverDisconnected().unsubscribe(myResponseReceiverDisconnected);
                        aDuplexInputChannelContext.getAttachedDuplexInputChannel().messageReceived().unsubscribe(myMessageReceivedHandler);

                        HashSetExt.removeWhere(myDuplexInputChannelContexts,
                                new IFunction1<Boolean, TDuplexInputChannelContext>()
                                {
                                    @Override
                                    public Boolean invoke(
                                            TDuplexInputChannelContext x)
                                            throws Exception
                                    {
                                        return x.getAttachedDuplexInputChannel().getChannelId() == aDuplexInputChannelContext.getAttachedDuplexInputChannel().getChannelId();
                                    }
                                });
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void detachDuplexInputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                for (TDuplexInputChannelContext aDuplexInputChannelContext : myDuplexInputChannelContexts)
                {
                    // Go via all connections with clients and close them.
                    closeConnections(aDuplexInputChannelContext.getOpenConnections());

                    try
                    {
                        aDuplexInputChannelContext.getAttachedDuplexInputChannel().stopListening();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to correctly detach the duplex input channel '" + aDuplexInputChannelContext.getAttachedDuplexInputChannel() + "'.", err);
                    }

                    aDuplexInputChannelContext.getAttachedDuplexInputChannel().responseReceiverDisconnected().unsubscribe(myResponseReceiverDisconnected);
                    aDuplexInputChannelContext.getAttachedDuplexInputChannel().messageReceived().unsubscribe(myMessageReceivedHandler);
                }

                myDuplexInputChannelContexts.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public boolean isDuplexInputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                return !myDuplexInputChannelContexts.isEmpty();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public Iterable<IDuplexInputChannel> getAttachedDuplexInputChannels()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: Because of thread safety, create a new container to store the references.
            ArrayList<IDuplexInputChannel> anAttachedChannels = new ArrayList<IDuplexInputChannel>();
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                for (TDuplexInputChannelContext aContextItem : myDuplexInputChannelContexts)
                {
                    anAttachedChannels.add(aContextItem.getAttachedDuplexInputChannel());
                }
            }

            return anAttachedChannels;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    protected void closeDuplexOutputChannel(final String duplexOutputChannelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                IFunction1<Boolean, TConnection> aPredicate = new IFunction1<Boolean, TConnection>()
                {
                    @Override
                    public Boolean invoke(TConnection x)
                            throws Exception
                    {
                        return x.getConnectedDuplexOutputChannel().getChannelId() == duplexOutputChannelId;
                    }
                };
                
                for (TDuplexInputChannelContext aDuplexInputChannelContext : myDuplexInputChannelContexts)
                {
                    Iterable<TConnection> aConnections = EnumerableExt.where(aDuplexInputChannelContext.getOpenConnections(), aPredicate);
                            
                    closeConnections(aConnections);
                    
                    HashSetExt.removeWhere(aDuplexInputChannelContext.getOpenConnections(), aPredicate);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    protected void sendMessage(String duplexInputChannelId, String duplexInputChannelResponseReceiverId, String duplexOutputChannelId, Object message)
        throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                try
                {
                    // Get (or create) the duplex output channel that will be used 
                    IDuplexOutputChannel aDuplexOutputChannel = getAssociatedDuplexOutputChannel(duplexInputChannelId, duplexInputChannelResponseReceiverId, duplexOutputChannelId);
                    aDuplexOutputChannel.sendMessage(message);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to send the message to the duplex output channel '" + duplexOutputChannelId + "'.", err);
                    throw err;
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + "failed to send the message to the duplex output channel '" + duplexOutputChannelId + "'.", err);
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    protected void sendResponseMessage(final String duplexOutputChannelResponseReceiverId, Object message)
        throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                final TConnection[] anAssociatedConnection = {null};
                TDuplexInputChannelContext aDuplexInputChannelContext = EnumerableExt.firstOrDefault(myDuplexInputChannelContexts,
                        new IFunction1<Boolean, TDuplexInputChannelContext>()
                        {
                            @Override
                            public Boolean invoke(TDuplexInputChannelContext x)
                                    throws Exception
                            {
                                anAssociatedConnection[0] = EnumerableExt.firstOrDefault(x.myOpenConnections,
                                        new IFunction1<Boolean, TConnection>()
                                        {
                                            @Override
                                            public Boolean invoke(TConnection xx)
                                                    throws Exception
                                            {
                                                return xx.getConnectedDuplexOutputChannel().getResponseReceiverId() == duplexOutputChannelResponseReceiverId;
                                            }
                                        });
                                        
                                return anAssociatedConnection != null;
                            }
                    
                        });
                        

                if (aDuplexInputChannelContext == null)
                {
                    String anError = TracedObject() + "failed to send the response message because the duplex input channel associated with the response was not found.";
                    EneterTrace.error(anError);
                    throw new IllegalStateException(anError);
                }

                if (anAssociatedConnection[0] == null)
                {
                    String anError = TracedObject() + "failed to send the response message because the duplex output channel with the given response receiver id was not found.";
                    EneterTrace.error(anError);
                    throw new IllegalStateException(anError);
                }

                try
                {
                    aDuplexInputChannelContext.getAttachedDuplexInputChannel().sendResponseMessage(anAssociatedConnection[0].getResponseReceiverId(), message);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to send the response message for the response receiver '" + anAssociatedConnection[0].getResponseReceiverId() + "' through the duplex input channel '" + aDuplexInputChannelContext.getAttachedDuplexInputChannel().getChannelId() + "'.", err);
                    throw err;
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + "failed to send the response message for the response receiver '" + anAssociatedConnection[0].getResponseReceiverId() + "' through the duplex input channel '" + aDuplexInputChannelContext.getAttachedDuplexInputChannel().getChannelId() + "'.", err);
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void attach(final IDuplexInputChannel duplexInputChannel)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                if (duplexInputChannel == null)
                {
                    String anError = TracedObject() + "failed to attach the duplex input channel because the input parameter 'duplexInputChannel' is null.";
                    EneterTrace.error(anError);
                    throw new IllegalArgumentException(anError);
                }

                if (StringExt.isNullOrEmpty(duplexInputChannel.getChannelId()))
                {
                    String anError = TracedObject() + "failed to attach the duplex input channel because the input parameter 'duplexInputChannel' has null or empty channel id.";
                    EneterTrace.error(anError);
                    throw new IllegalArgumentException(anError);
                }

                // If the channel with the same id is already attached then throw the exception.
                if (EnumerableExt.any(myDuplexInputChannelContexts,
                        new IFunction1<Boolean, TDuplexInputChannelContext>()
                        {
                            @Override
                            public Boolean invoke(TDuplexInputChannelContext x)
                                    throws Exception
                            {
                                return x.getAttachedDuplexInputChannel().getChannelId() == duplexInputChannel.getChannelId();
                            }
                            
                        }))
                {
                    String anError = TracedObject() + "failed to attach the duplex input channel '" + duplexInputChannel.getChannelId() + "' because the duplex input channel with the same id is already attached.";
                    EneterTrace.error(anError);
                    throw new IllegalStateException(anError);
                }

                myDuplexInputChannelContexts.add(new TDuplexInputChannelContext(duplexInputChannel));

                // Start listening to the attached channel.
                duplexInputChannel.responseReceiverDisconnected().subscribe(myResponseReceiverDisconnected);
                duplexInputChannel.messageReceived().subscribe(myMessageReceivedHandler);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private IDuplexOutputChannel getAssociatedDuplexOutputChannel(final String duplexInputChannelId, final String responseReceiverId, final String duplexOutputChannelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                TDuplexInputChannelContext aDuplexInputChannelContext = EnumerableExt.firstOrDefault(myDuplexInputChannelContexts,
                        new IFunction1<Boolean, TDuplexInputChannelContext>()
                        {
                            @Override
                            public Boolean invoke(TDuplexInputChannelContext x)
                                    throws Exception
                            {
                                return x.getAttachedDuplexInputChannel().getChannelId() == duplexInputChannelId;
                            }
                        });
                        
                if (aDuplexInputChannelContext == null)
                {
                    String anError = TracedObject() + "failed to return the duplex output channel associated with the duplex input channel '" + duplexInputChannelId + "' because the duplex input channel was not attached.";
                    EneterTrace.error(anError);
                    throw new IllegalStateException(anError);
                }

                TConnection aConnection = EnumerableExt.firstOrDefault(aDuplexInputChannelContext.getOpenConnections(),
                        new IFunction1<Boolean, TConnection>()
                        {
                            @Override
                            public Boolean invoke(TConnection x)
                                    throws Exception
                            {
                                return x.getResponseReceiverId() == responseReceiverId && x.getConnectedDuplexOutputChannel().getChannelId() == duplexOutputChannelId;
                            }
                        });
                        
                if (aConnection == null)
                {
                    IDuplexOutputChannel anAssociatedDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(duplexOutputChannelId);

                    try
                    {
                        anAssociatedDuplexOutputChannel.responseMessageReceived().subscribe(myResponseMessageReceivedHandler);
                        anAssociatedDuplexOutputChannel.openConnection();
                    }
                    catch (Exception err)
                    {
                        anAssociatedDuplexOutputChannel.responseMessageReceived().unsubscribe(myResponseMessageReceivedHandler);

                        EneterTrace.error(TracedObject() + "failed to open connection for the duplex output channel '" + duplexOutputChannelId + "'.", err);
                        throw err;
                    }


                    aConnection = new TConnection(responseReceiverId, anAssociatedDuplexOutputChannel);
                    aDuplexInputChannelContext.getOpenConnections().add(aConnection);
                }

                return aConnection.getConnectedDuplexOutputChannel();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    /// <summary>
    /// Closes given connections with client duplex output channel.
    /// </summary>
    /// <param name="connections"></param>
    private void closeConnections(Iterable<TConnection> connections)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                for (TConnection aConnection : connections)
                {
                    try
                    {
                        aConnection.getConnectedDuplexOutputChannel().closeConnection();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to close correctly the connection for the duplex output channel '" + aConnection.getConnectedDuplexOutputChannel().getChannelId() + "'.", err);
                    }

                    aConnection.getConnectedDuplexOutputChannel().responseMessageReceived().unsubscribe(myResponseMessageReceivedHandler);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void onDuplexInputChannelResponseReceiverDisconnected(Object sender, final ResponseReceiverEventArgs e)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannelContextManipulatorLock)
            {
                IFunction1<Boolean, TConnection> aPredicate = new IFunction1<Boolean, AttachableMultipleDuplexInputChannelsBase.TConnection>()
                {
                    @Override
                    public Boolean invoke(TConnection x)
                            throws Exception
                    {
                        return x.getResponseReceiverId() == e.getResponseReceiverId();
                    }
                };
                
                for (TDuplexInputChannelContext aDuplexInputChannelContext : myDuplexInputChannelContexts)
                {
                    Iterable<TConnection> aConnections = EnumerableExt.where(aDuplexInputChannelContext.getOpenConnections(), aPredicate);
                    closeConnections(aConnections);
                    HashSetExt.removeWhere(aDuplexInputChannelContext.getOpenConnections(), aPredicate);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    protected abstract void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e);
    protected abstract void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e);

    protected IMessagingSystemFactory getMessagingSystemFactory()
    {
        return myMessagingSystemFactory;
    }

    private Object myDuplexInputChannelContextManipulatorLock = new Object();
    private HashSet<TDuplexInputChannelContext> myDuplexInputChannelContexts = new HashSet<TDuplexInputChannelContext>();

    private IMessagingSystemFactory myMessagingSystemFactory;
    
    private IMethod2<Object, DuplexChannelMessageEventArgs> myMessageReceivedHandler = new IMethod2<Object, DuplexChannelMessageEventArgs>()
    {
        @Override
        public void invoke(Object sender, DuplexChannelMessageEventArgs e)
                throws Exception
        {
            onMessageReceived(sender, e);
        }
    };
    
    private IMethod2<Object, DuplexChannelMessageEventArgs> myResponseMessageReceivedHandler = new IMethod2<Object, DuplexChannelMessageEventArgs>()
    {
        @Override
        public void invoke(Object sender, DuplexChannelMessageEventArgs e)
                throws Exception
        {
            onResponseMessageReceived(sender, e);
        }
    };
    
    private IMethod2<Object, ResponseReceiverEventArgs> myResponseReceiverDisconnected = new IMethod2<Object, ResponseReceiverEventArgs>()
    {
        @Override
        public void invoke(Object sender, ResponseReceiverEventArgs e)
                throws Exception
        {
            onDuplexInputChannelResponseReceiverDisconnected(sender, e);
        }
    };

    protected abstract String TracedObject();
}

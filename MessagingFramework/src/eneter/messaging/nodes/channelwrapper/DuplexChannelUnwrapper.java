/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import java.util.*;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.wrapping.*;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.*;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.HashSetExt;
import eneter.net.system.internal.IFunction1;
import eneter.net.system.linq.internal.*;

class DuplexChannelUnwrapper extends AttachableDuplexInputChannelBase
                             implements IDuplexChannelUnwrapper
{
    private class TDuplexConnection
    {
        public TDuplexConnection(String responseReceiverId, IDuplexOutputChannel duplexOutputChannel)
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
        
        private String myResponseReceiverId;
        private IDuplexOutputChannel myDuplexOutputChannel;
    }
    
    public DuplexChannelUnwrapper(IMessagingSystemFactory outputMessagingFactory, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myOutputMessagingFactory = outputMessagingFactory;
            mySerializer = serializer;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
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
    protected void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            WrappedData aWrappedData = null;

            try
            {
                // Unwrap the incoming message.
                aWrappedData = DataWrapper.unwrap(e.getMessage(), mySerializer);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to unwrap the message.", err);
                return;
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + "failed to unwrap the message.", err);
                throw err;
            }

            // WrappedData.AddedData represents the channel id.
            // Therefore if everything is ok then it must be string.
            if (aWrappedData.AddedData instanceof String)
            {
                final DuplexChannelMessageEventArgs ee = e;
                final String aMessageReceiverId = (String)aWrappedData.AddedData;

                TDuplexConnection aConectionToOutput = null;

                // Try to find if the output channel with the required channel id and for the incoming response receiver
                // already exists.
                synchronized (myConnections)
                {
                    aConectionToOutput = EnumerableExt.firstOrDefault(myConnections, new IFunction1<Boolean, TDuplexConnection>()
                    {
                        @Override
                        public Boolean invoke(TDuplexConnection x)
                                throws Exception
                        {
                            return x.getDuplexOutputChannel().getChannelId().equals(aMessageReceiverId) &&
                                   x.getResponseReceiverId().equals(ee.getResponseReceiverId());
                        }
                    });

                    // If it does not exist then create the duplex output channel and open connection.
                    if (aConectionToOutput == null)
                    {
                        IDuplexOutputChannel aDuplexOutputChannel = null;

                        try
                        {
                            aDuplexOutputChannel = myOutputMessagingFactory.createDuplexOutputChannel(aMessageReceiverId);
                            aDuplexOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceivedHandler);
                            aDuplexOutputChannel.openConnection();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error(TracedObject() + "failed to create and connect the duplex output channel '" + aMessageReceiverId + "'", err);

                            if (aDuplexOutputChannel != null)
                            {
                                aDuplexOutputChannel.responseMessageReceived().unsubscribe(myOnResponseMessageReceivedHandler);
                                aDuplexOutputChannel.closeConnection();
                                aDuplexOutputChannel = null;
                            }
                        }

                        if (aDuplexOutputChannel != null)
                        {
                            aConectionToOutput = new TDuplexConnection(e.getResponseReceiverId(), aDuplexOutputChannel);
                            myConnections.add(aConectionToOutput);
                        }
                    }
                }

                if (aConectionToOutput != null)
                {
                    try
                    {
                        // Send the unwrapped message.
                        aConectionToOutput.getDuplexOutputChannel().sendMessage(aWrappedData.OriginalData);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + "failed to send the message to the output channel '" + aConectionToOutput.getDuplexOutputChannel().getChannelId() + "'.", err);
                    }
                    catch (Error err)
                    {
                        EneterTrace.error(TracedObject() + "failed to send the message to the output channel '" + aConectionToOutput.getDuplexOutputChannel().getChannelId() + "'.", err);
                        throw err;
                    }
                }
            }
            else
            {
                EneterTrace.error(TracedObject() + "detected that the unwrapped message contian the channel id as the string type.");
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "detected exception when message received.", err);
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
                catch (Error err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    throw err;
                }
            }
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
            synchronized (myConnections)
            {
                final ResponseReceiverEventArgs ee = e;
                
                Iterable<TDuplexConnection> aConnections = EnumerableExt.where(myConnections, new IFunction1<Boolean, TDuplexConnection>()
                        {
                            @Override
                            public Boolean invoke(TDuplexConnection x)
                                    throws Exception
                            {
                                return x.getResponseReceiverId().equals(ee.getResponseReceiverId());
                            }
                    
                        });
                        
                for (TDuplexConnection aConnection : aConnections)
                {
                    try
                    {
                        aConnection.getDuplexOutputChannel().responseMessageReceived().unsubscribe(myOnResponseMessageReceivedHandler);
                        aConnection.getDuplexOutputChannel().closeConnection();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                    }

                }

                HashSetExt.removeWhere(myConnections, new IFunction1<Boolean, TDuplexConnection>()
                        {
                            @Override
                            public Boolean invoke(TDuplexConnection x)
                                    throws Exception
                            {
                                return x.getResponseReceiverId().equals(ee.getResponseReceiverId());
                            }
                    
                        });
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
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                    throw err;
                }
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "detected exception response receiver disconnected.", err);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                final Object aSender = sender;
                
                // try to find the response receiver id where the wrapped message should be responded.
                TDuplexConnection aConnction = null;
                synchronized (myConnections)
                {
                    aConnction = EnumerableExt.firstOrDefault(myConnections, new IFunction1<Boolean, TDuplexConnection>()
                            {
                                @Override
                                public Boolean invoke(TDuplexConnection x)
                                        throws Exception
                                {
                                    return x.getDuplexOutputChannel() == (IDuplexOutputChannel)aSender;
                                }
                            });
                }

                if (aConnction != null)
                {
                    Object aMessage = DataWrapper.wrap(e.getChannelId(), e.getMessage(), mySerializer);
                    getAttachedDuplexInputChannel().sendResponseMessage(aConnction.getResponseReceiverId(), aMessage);
                }
                else
                {
                    EneterTrace.warning(TracedObject() + "failed to send the response message because the response receiver id does not exist. It is possible the response receiver has already been disconnected.");
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingSystemFactory myOutputMessagingFactory;
    private ISerializer mySerializer;

    private HashSet<TDuplexConnection> myConnections = new HashSet<TDuplexConnection>();
    
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
        String aDuplexInputChannelId = (getAttachedDuplexInputChannel() != null) ? getAttachedDuplexInputChannel().getChannelId() : "";
        return "The DuplexChannelUnwrapper attached to the duplex input channel '" + aDuplexInputChannelId + "' ";
    }

}

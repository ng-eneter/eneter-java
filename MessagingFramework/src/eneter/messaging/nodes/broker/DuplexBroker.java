/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import java.util.*;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;


class DuplexBroker extends AttachableDuplexInputChannelBase implements IDuplexBroker
{
    private class TSubscription
    {
        public TSubscription(String messageTypeId, String receiverId)
        {
            MessageTypeId = messageTypeId;
            ReceiverId = receiverId;
        }

        public final String MessageTypeId;
        public final String ReceiverId;
    }
    
    @Override
    public Event<BrokerMessageReceivedEventArgs> brokerMessageReceived()
    {
        return myBrokerMessageReceivedEvent.getApi();
    }
    
    public DuplexBroker(boolean isPublisherNotified, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myIsPublisherSelfnotified = isPublisherNotified;
            mySerializer = serializer;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void sendMessage(String eventId, Object serializedMessage)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            BrokerMessage aNotifyMessage = new BrokerMessage(eventId, serializedMessage);
            Object aSerializedNotifyMessage = mySerializer.serialize(aNotifyMessage, BrokerMessage.class);
            publish(myLocalReceiverId, aNotifyMessage, aSerializedNotifyMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void subscribe(String eventId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String[] aEventsToSubscribe = { eventId };
            subscribe(myLocalReceiverId, aEventsToSubscribe);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void subscribe(String[] eventIds) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            subscribe(myLocalReceiverId, eventIds);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void unsubscribe(String eventId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String[] aEventsToUnsubscribe = { eventId };
            unsubscribe(myLocalReceiverId, aEventsToUnsubscribe);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void unsubscribe(String[] eventIds) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            unsubscribe(myLocalReceiverId, eventIds);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void unsubscribe() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            unsubscribe(myLocalReceiverId, null);
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
            // Try to deserialize the message.
            BrokerMessage aBrokerMessage;
            try
            {
                aBrokerMessage = mySerializer.deserialize(e.getMessage(), BrokerMessage.class);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize the message.", err);
                return;
            }


            if (aBrokerMessage.Request == EBrokerRequest.Publish)
            {
                publish(e.getResponseReceiverId(), aBrokerMessage, e.getMessage());
            }
            else if (aBrokerMessage.Request == EBrokerRequest.Subscribe)
            {
                subscribe(e.getResponseReceiverId(), aBrokerMessage.MessageTypes);
            }
            else if (aBrokerMessage.Request == EBrokerRequest.Unsubscribe)
            {
                unsubscribe(e.getResponseReceiverId(), aBrokerMessage.MessageTypes);
            }
            else if (aBrokerMessage.Request == EBrokerRequest.UnsubscribeAll)
            {
                unsubscribe(e.getResponseReceiverId(), null);
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        // n.a.
    }
    
    @Override
    protected void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            unsubscribe(e.getResponseReceiverId(), null);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void publish(final String publisherResponseReceiverId, final BrokerMessage message, Object originalSerializedMessage)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ArrayList<TSubscription> anIdetifiedSubscriptions = new ArrayList<TSubscription>();
            
            synchronized (mySubscribtions)
            {
                for (TSubscription aMessageSubscription : mySubscribtions)
                {
                    if ((myIsPublisherSelfnotified || !aMessageSubscription.ReceiverId.equals(publisherResponseReceiverId)) &&
                        aMessageSubscription.MessageTypeId.equals(message.MessageTypes[0]))
                    {
                        anIdetifiedSubscriptions.add(aMessageSubscription);
                    }
                }
            }
            
            for (TSubscription aSubscription : anIdetifiedSubscriptions)
            {
                if (aSubscription.ReceiverId.equals(myLocalReceiverId))
                {
                    if (myBrokerMessageReceivedEvent.isSubscribed())
                    {
                        try
                        {
                            BrokerMessageReceivedEventArgs anEvent = new BrokerMessageReceivedEventArgs(message.MessageTypes[0], message.Message);
                            myBrokerMessageReceivedEvent.raise(this, anEvent);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                        }
                    }
                }
                else
                {
                    send(aSubscription.ReceiverId, originalSerializedMessage);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void send(String responseReceiverId, Object serializedMessage) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexInputChannel anAttachedInputChannel = getAttachedDuplexInputChannel();
            if (anAttachedInputChannel == null)
            {
                String anErrorMessage = TracedObject() + "failed to send the message because the it is not attached to duplex input channel.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            try
            {
                anAttachedInputChannel.sendResponseMessage(responseReceiverId, serializedMessage);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to send the message. The client will be disconnected and unsubscribed from all messages.", err);

                try
                {
                    // Try to disconnect the client.
                    anAttachedInputChannel.disconnectResponseReceiver(responseReceiverId);
                }
                catch (Exception err2)
                {
                }

                // Unsubscribe the failed client.
                unsubscribe(responseReceiverId, null);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void subscribe(String responseReceiverId, String[] messageTypes)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (mySubscribtions)
            {
                // Subscribe only messages that are not subscribed yet.
                ArrayList<String> aMessagesToSubscribe = new ArrayList<String>(Arrays.asList(messageTypes));
                for (TSubscription aSubscription : mySubscribtions)
                {
                    if (aSubscription.ReceiverId.equals(responseReceiverId))
                    {
                        aMessagesToSubscribe.remove(aSubscription.MessageTypeId);
                    }
                }
                
                // Subscribe
                for (String aMessageType : aMessagesToSubscribe)
                {
                    mySubscribtions.add(new TSubscription(aMessageType, responseReceiverId));
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void unsubscribe(final String responseReceiverId, final String[] messageTypes)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (mySubscribtions)
            {
                // If unsubscribe from all messages
                if (messageTypes == null || messageTypes.length == 0)
                {
                    try
                    {
                        HashSetExt.removeWhere(mySubscribtions, new IFunction1<Boolean, TSubscription>()
                            {
                                @Override
                                public Boolean invoke(TSubscription x)
                                        throws Exception
                                {
                                    return x.ReceiverId.equals(responseReceiverId);
                                }
                        
                            });
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + "failed to unregister subscriber.", err);
                    }
                }
                // If unsubscribe from specified messages
                else
                {
                    try
                    {
                        HashSetExt.removeWhere(mySubscribtions, new IFunction1<Boolean, TSubscription>()
                            {
                                @Override
                                public Boolean invoke(TSubscription x)
                                        throws Exception
                                {
                                    if (x.ReceiverId.equals(responseReceiverId))
                                    {
                                        // If it is one of messages that should be unsubscribed then return
                                        // true indicating the item shall be removed.
                                        for (String aMessageType : messageTypes)
                                        {
                                            if (aMessageType.equals(x.MessageTypeId))
                                            {
                                                return true;
                                            }
                                        }
                                    }
                                    
                                    return false;
                                }
                        
                            });
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + "failed to unregister subscription.", err);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private HashSet<TSubscription> mySubscribtions = new HashSet<TSubscription>();
    private boolean myIsPublisherSelfnotified;
    private ISerializer mySerializer;
    
    private final String myLocalReceiverId = "Eneter.Broker.LocalReceiver";
    
    
    private EventImpl<BrokerMessageReceivedEventArgs> myBrokerMessageReceivedEvent = new EventImpl<BrokerMessageReceivedEventArgs>();
    
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import java.util.*;

import eneter.messaging.dataprocessing.serializing.*;
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
    public Event<PublishInfoEventArgs> messagePublished()
    {
        return myMessagePublishedEvent.getApi();
    }

    @Override
    public Event<SubscribeInfoEventArgs> clientSubscribed()
    {
        return myClientSubscribedEvent.getApi();
    }

    @Override
    public Event<SubscribeInfoEventArgs> clientUnsubscribed()
    {
        return myClientUnsubscribedEvent.getApi();
    }
    
    @Override
    public Event<BrokerMessageReceivedEventArgs> brokerMessageReceived()
    {
        return myBrokerMessageReceivedEvent.getApi();
    }
    
    public DuplexBroker(boolean isPublisherNotified, ISerializer serializer,
            GetSerializerCallback getSerializerCallback,
            AuthorizeBrokerRequestCallback validateBrokerRequestCallback)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myIsPublisherSelfnotified = isPublisherNotified;
            mySerializer = serializer;
            myGetSerializerCallback = getSerializerCallback;
            myValidateBrokerRequestCallback = validateBrokerRequestCallback;
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
            
            // If one serializer is used for the whole communication then pre-serialize the message to increase the performance.
            // If there is SerializerProvider callback then the serialization must be performed before sending individualy
            // for each client.
            Object aSerializedNotifyMessage = (myGetSerializerCallback == null) ?
                mySerializer.serialize(aNotifyMessage, BrokerMessage.class) :
                null;
            
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
            ArrayList<String> anUnsubscribedMessages = unsubscribe(myLocalReceiverId, aEventsToUnsubscribe);
            raiseClientUnsubscribed(myLocalReceiverId, anUnsubscribedMessages);
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
            ArrayList<String> anUnsubscribedMessages = unsubscribe(myLocalReceiverId, eventIds);
            raiseClientUnsubscribed(myLocalReceiverId, anUnsubscribedMessages);
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
            ArrayList<String> anUnsubscribedMessages = unsubscribe(myLocalReceiverId, null);
            raiseClientUnsubscribed(myLocalReceiverId, anUnsubscribedMessages);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public String[] getSubscribedMessages(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ArrayList<String> aResult = new ArrayList<String>();
            
            synchronized (mySubscribtions)
            {
                for (TSubscription x : mySubscribtions)
                {
                    if (x.ReceiverId.equals(responseReceiverId))
                    {
                        aResult.add(x.MessageTypeId);
                    }
                }
            }
            
            String[] aResultArray = new String[aResult.size()];
            aResultArray = aResult.toArray(aResultArray);
            return aResultArray;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public String[] GetSubscribedResponseReceivers(String messageTypeId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ArrayList<String> aResult = new ArrayList<String>();
            
            synchronized (mySubscribtions)
            {
                for (TSubscription x : mySubscribtions)
                {
                    if (x.MessageTypeId.equals(messageTypeId))
                    {
                        aResult.add(x.MessageTypeId);
                    }
                }
            }
            
            String[] aResultArray = new String[aResult.size()];
            aResultArray = aResult.toArray(aResultArray);
            return aResultArray;
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
                ISerializer aSerializer = (myGetSerializerCallback == null) ? mySerializer : myGetSerializerCallback.invoke(e.getResponseReceiverId());
                aBrokerMessage = aSerializer.deserialize(e.getMessage(), BrokerMessage.class);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize the message.", err);
                return;
            }

            if (myValidateBrokerRequestCallback != null)
            {
                boolean isValidated = false;
                try
                {
                    isValidated = myValidateBrokerRequestCallback.invoke(e.getResponseReceiverId(), aBrokerMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                }

                if (!isValidated)
                {
                    ArrayList<String> anUnsubscribedMessages = unsubscribe(e.getResponseReceiverId(), null);
                    raiseClientUnsubscribed(e.getResponseReceiverId(), anUnsubscribedMessages);

                    try
                    {
                        getAttachedDuplexInputChannel().disconnectResponseReceiver(e.getResponseReceiverId());
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to disconnect response receiver.", err);
                    }
                    return;
                }
            }

            if (aBrokerMessage.Request == EBrokerRequest.Publish)
            {
                if (myGetSerializerCallback == null)
                {
                    // If only one serializer is used for communication with all clients then
                    // increase the performance by reusing already serialized message.
                    publish(e.getResponseReceiverId(), aBrokerMessage, e.getMessage());
                }
                else
                {
                    // If there is a serializer per client then the message must be serialized
                    // individually for each subscribed client.
                    publish(e.getResponseReceiverId(), aBrokerMessage, null);
                }
            }
            else if (aBrokerMessage.Request == EBrokerRequest.Subscribe)
            {
                subscribe(e.getResponseReceiverId(), aBrokerMessage.MessageTypes);
            }
            else if (aBrokerMessage.Request == EBrokerRequest.Unsubscribe)
            {
                ArrayList<String> anUnsubscribedMessages = unsubscribe(e.getResponseReceiverId(), aBrokerMessage.MessageTypes);
                raiseClientUnsubscribed(e.getResponseReceiverId(), anUnsubscribedMessages);
            }
            else if (aBrokerMessage.Request == EBrokerRequest.UnsubscribeAll)
            {
                ArrayList<String> anUnsubscribedMessages = unsubscribe(e.getResponseReceiverId(), null);
                raiseClientUnsubscribed(e.getResponseReceiverId(), anUnsubscribedMessages);
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
            ArrayList<String> anUnsubscribedMessages = unsubscribe(e.getResponseReceiverId(), null);
            raiseClientUnsubscribed(e.getResponseReceiverId(), anUnsubscribedMessages);
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
            
            HashMap<String, ArrayList<String>> aFailedSubscribers = new HashMap<String, ArrayList<String>>();
            int aNumberOfSentSubscribers = 0;
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
                        
                        ++aNumberOfSentSubscribers;
                    }
                }
                else
                {
                    Object aSerializedMessage = originalSerializedMessage;
                    if (aSerializedMessage == null)
                    {
                        try
                        {
                            ISerializer aSerializer = myGetSerializerCallback.invoke(aSubscription.ReceiverId);
                            aSerializedMessage = aSerializer.serialize(message, BrokerMessage.class);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error(TracedObject() + "failed to serialize BrokerMessage using GetSerializeCallback.", err);
                        }
                    }
                    
                    if (aSerializedMessage != null)
                    {
                        ArrayList<String> anUnsubscribedMessagesDueToFailure = send(aSubscription.ReceiverId, aSerializedMessage);
                        if (anUnsubscribedMessagesDueToFailure.size() > 0)
                        {
                            aFailedSubscribers.put(aSubscription.ReceiverId, anUnsubscribedMessagesDueToFailure);
                        }
                        else
                        {
                            ++aNumberOfSentSubscribers;
                        }
                    }
                }
            }
            
            if (myMessagePublishedEvent.isSubscribed())
            {
                PublishInfoEventArgs anEvent = new PublishInfoEventArgs(publisherResponseReceiverId, message.MessageTypes[0], message.Message, aNumberOfSentSubscribers);
                try
                {
                    myMessagePublishedEvent.raise(this, anEvent);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }

            // If sending to some subscribers failed then they were unsubscribed.
            for(Map.Entry<String, ArrayList<String>> aFailedSubscriber : aFailedSubscribers.entrySet())
            {
                raiseClientUnsubscribed(aFailedSubscriber.getKey(), aFailedSubscriber.getValue());
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private ArrayList<String> send(String responseReceiverId, Object serializedMessage) throws Exception
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
                return unsubscribe(responseReceiverId, null);
            }
            
            return new ArrayList<String>();
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
            ArrayList<String> aMessagesToSubscribe = new ArrayList<String>(Arrays.asList(messageTypes));
            
            synchronized (mySubscribtions)
            {
                // Subscribe only messages that are not subscribed yet.
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
            
            if (myClientSubscribedEvent.isSubscribed() && aMessagesToSubscribe.size() > 0)
            {
                String[] aMessagesToSubscribeArray = new String[aMessagesToSubscribe.size()];
                aMessagesToSubscribeArray = aMessagesToSubscribe.toArray(aMessagesToSubscribeArray);
                SubscribeInfoEventArgs anEvent = new SubscribeInfoEventArgs(responseReceiverId, aMessagesToSubscribeArray);
                try
                {
                    myClientSubscribedEvent.raise(this, anEvent);
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
    
    private ArrayList<String> unsubscribe(final String responseReceiverId, final String[] messageTypes)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            final ArrayList<String> anUnsubscribedMessages = new ArrayList<String>();
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
                                    if (x.ReceiverId.equals(responseReceiverId))
                                    {
                                        anUnsubscribedMessages.add(x.MessageTypeId);
                                        return true;
                                    }
                                    
                                    return false;
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
                                                anUnsubscribedMessages.add(x.MessageTypeId);
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
                
                return anUnsubscribedMessages;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void raiseClientUnsubscribed(String responseReceiverId, ArrayList<String> messageTypeIds)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myClientUnsubscribedEvent.isSubscribed() && messageTypeIds != null && messageTypeIds.size() > 0)
            {
                String[] anMessageTypeIds = new String[messageTypeIds.size()];
                anMessageTypeIds = messageTypeIds.toArray(anMessageTypeIds);
                SubscribeInfoEventArgs anEvent = new SubscribeInfoEventArgs(responseReceiverId, anMessageTypeIds);
                try
                {
                    myClientUnsubscribedEvent.raise(this, anEvent);
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
    
    
    private HashSet<TSubscription> mySubscribtions = new HashSet<TSubscription>();
    private boolean myIsPublisherSelfnotified;
    private ISerializer mySerializer;
    private GetSerializerCallback myGetSerializerCallback;
    private AuthorizeBrokerRequestCallback myValidateBrokerRequestCallback;
    
    private final String myLocalReceiverId = "Eneter.Broker.LocalReceiver";
    
    private EventImpl<PublishInfoEventArgs> myMessagePublishedEvent = new EventImpl<PublishInfoEventArgs>();
    private EventImpl<SubscribeInfoEventArgs> myClientSubscribedEvent = new EventImpl<SubscribeInfoEventArgs>();
    private EventImpl<SubscribeInfoEventArgs> myClientUnsubscribedEvent = new EventImpl<SubscribeInfoEventArgs>();
    private EventImpl<BrokerMessageReceivedEventArgs> myBrokerMessageReceivedEvent = new EventImpl<BrokerMessageReceivedEventArgs>();
    
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

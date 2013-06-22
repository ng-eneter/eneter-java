/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import java.util.*;
import java.util.regex.Pattern;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;
import eneter.net.system.linq.internal.EnumerableExt;

class DuplexBroker extends AttachableDuplexInputChannelBase implements IDuplexBroker
{
    private class TSubscriptionItem
    {
        public TSubscriptionItem(String messageTypeId, String receiverId)
        {
            myMessageTypeId = messageTypeId;
            myReceiverId = receiverId;
        }

        // MessageTypeId or regular expression used to recognize matching message type id.
        public String getMessageTypeId()
        {
            return myMessageTypeId;
        }
        
        public String getReceiverId()
        {
            return myReceiverId;
        }
        
        private String myMessageTypeId;
        private String myReceiverId;
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
            subscribe(myLocalReceiverId, aEventsToSubscribe, myMessageSubscribtions);
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
            subscribe(myLocalReceiverId, eventIds, myMessageSubscribtions);
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
            unsubscribe(myLocalReceiverId, aEventsToUnsubscribe, myMessageSubscribtions);
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
            unsubscribe(myLocalReceiverId, eventIds, myMessageSubscribtions);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void subscribeRegExp(String regularExpression) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String[] aSubscribingExpressions = { regularExpression };
            subscribe(myLocalReceiverId, aSubscribingExpressions, myRegExpSubscribtions);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void subscribeRegExp(String[] regularExpressions) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            subscribe(myLocalReceiverId, regularExpressions, myRegExpSubscribtions);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void unsubscribeRegExp(String regularExpression) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String[] aUnsubscribingExpressions = { regularExpression };
            unsubscribe(myLocalReceiverId, aUnsubscribingExpressions, myRegExpSubscribtions);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void unsubscribeRegExp(String[] regularExpressions) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            unsubscribe(myLocalReceiverId, regularExpressions, myRegExpSubscribtions);
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
            synchronized (mySubscribtionManipulatorLock)
            {
                unsubscribe(myLocalReceiverId, null, myMessageSubscribtions);
                unsubscribe(myLocalReceiverId, null, myRegExpSubscribtions);
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
                subscribe(e.getResponseReceiverId(), aBrokerMessage.MessageTypes, myMessageSubscribtions);
            }
            else if (aBrokerMessage.Request == EBrokerRequest.SubscribeRegExp)
            {
                subscribe(e.getResponseReceiverId(), aBrokerMessage.MessageTypes, myRegExpSubscribtions);
            }
            else if (aBrokerMessage.Request == EBrokerRequest.Unsubscribe)
            {
                unsubscribe(e.getResponseReceiverId(), aBrokerMessage.MessageTypes, myMessageSubscribtions);
            }
            else if (aBrokerMessage.Request == EBrokerRequest.UnsubscribeRegExp)
            {
                unsubscribe(e.getResponseReceiverId(), aBrokerMessage.MessageTypes, myRegExpSubscribtions);
            }
            else if (aBrokerMessage.Request == EBrokerRequest.UnsubscribeAll)
            {
                synchronized (mySubscribtionManipulatorLock)
                {
                    unsubscribe(e.getResponseReceiverId(), null, myMessageSubscribtions);
                    unsubscribe(e.getResponseReceiverId(), null, myRegExpSubscribtions);
                }
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
    protected void onResponseReceiverConnected(Object sender,
            ResponseReceiverEventArgs e)
    {
        // n.a.
    }
    
    @Override
    protected void onResponseReceiverDisconnected(Object sender,
            ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Remove all subscriptions for the disconnected subscriber.
            synchronized (mySubscribtionManipulatorLock)
            {
                unsubscribe(e.getResponseReceiverId(), null, myMessageSubscribtions);
                unsubscribe(e.getResponseReceiverId(), null, myRegExpSubscribtions);
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "detected exception when subscriber disconnected.", err);
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
            synchronized (mySubscribtionManipulatorLock)
            {
                final ArrayList<TSubscriptionItem> anIncorrectRegExpCollector = new ArrayList<TSubscriptionItem>();
                Iterable<TSubscriptionItem> aMessageSubscribers = EnumerableExt.where(myMessageSubscribtions, new IFunction1<Boolean, TSubscriptionItem>()
                        {
                            @Override
                            public Boolean invoke(TSubscriptionItem x) throws Exception
                            {
                                return (myIsPublisherSelfnotified || x.getReceiverId().equals(publisherResponseReceiverId)) && 
                                        x.getMessageTypeId().equals(message.MessageTypes[0]);
                            }
                        });


                Iterable<TSubscriptionItem> aRegExpSubscribers = EnumerableExt.where(myRegExpSubscribtions, new IFunction1<Boolean, TSubscriptionItem>()
                    {
                        @Override
                        public Boolean invoke(TSubscriptionItem x) throws Exception
                        {
                            if (myIsPublisherSelfnotified || x.getReceiverId().equals(publisherResponseReceiverId))
                            {
                                try
                                {
                                    String aRegEx = x.getMessageTypeId();
                                    String aMessageType = message.MessageTypes[0];
                                    boolean aMatchResult = Pattern.matches(aRegEx, aMessageType);
                                    return aMatchResult;
                                }
                                catch (Exception err)
                                {
                                    // The regular expression provided by a client can be incorrect and can cause an exception.
                                    // Other clients should not be affected by this. Therefore, we catch the exception and remove the invalid expression.
                                    EneterTrace.error(TracedObject() + "detected an incorrect regular expression: " + x.getMessageTypeId(), err);
    
                                    // Store the subscribtion with the wrong expression.
                                    anIncorrectRegExpCollector.add(x);
    
                                    return false;
                                }
                            }
                            else
                            {
                                return false;
                            }
                        }
                    }); 
                
                // Remove subscriptions with regular expresions causing exceptions.
                for (TSubscriptionItem x : anIncorrectRegExpCollector)
                {
                    myRegExpSubscribtions.remove(x);
                }

                // Notify subscribers
                sendNotifyMessages(message, originalSerializedMessage, aMessageSubscribers);
                
                // Notify subscribers subscribed via the regular expression.
                sendNotifyMessages(message, originalSerializedMessage, aRegExpSubscribers);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void sendNotifyMessages(BrokerMessage brokerMessage, Object serializedMessage, Iterable<TSubscriptionItem> subscribers)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            for (TSubscriptionItem aSubscriber : subscribers)
            {
                if (aSubscriber.getReceiverId().equals(myLocalReceiverId))
                {
                    if (myBrokerMessageReceivedEvent.isSubscribed())
                    {
                        try
                        {
                            BrokerMessageReceivedEventArgs anEvent = new BrokerMessageReceivedEventArgs(brokerMessage.MessageTypes[0], brokerMessage.Message);
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
                    send(aSubscriber.getReceiverId(), serializedMessage);
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
                synchronized (myDuplexInputChannelManipulatorLock)
                {
                    unsubscribe(responseReceiverId, null, myMessageSubscribtions);
                    unsubscribe(responseReceiverId, null, myRegExpSubscribtions);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void subscribe(String responseReceiverId, String[] messageTypes, HashSet<TSubscriptionItem> subscribtions)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (mySubscribtionManipulatorLock)
            {
                // Subscribe only messages that are not subscribed yet.
                ArrayList<String> aMessagesToSubscribe = new ArrayList<String>(Arrays.asList(messageTypes));
                for (TSubscriptionItem aSubscription : subscribtions)
                {
                    if (aSubscription.getReceiverId().equals(responseReceiverId))
                    {
                        aMessagesToSubscribe.remove(aSubscription.getMessageTypeId());
                    }
                }
                
                // Subscribe
                for (String aMessageType : aMessagesToSubscribe)
                {
                    subscribtions.add(new TSubscriptionItem(aMessageType, responseReceiverId));
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void unsubscribe(final String responseReceiverId, final String[] messageTypes, HashSet<TSubscriptionItem> subscribtions) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (mySubscribtionManipulatorLock)
            {
                // If unsubscribe from all messages
                if (messageTypes == null || messageTypes.length == 0)
                {
                    HashSetExt.removeWhere(subscribtions, new IFunction1<Boolean, TSubscriptionItem>()
                            {
                                @Override
                                public Boolean invoke(TSubscriptionItem x)
                                        throws Exception
                                {
                                    return x.getReceiverId().equals(responseReceiverId);
                                }
                        
                            });
                }
                // If unsubscribe from specified messages
                else
                {
                    HashSetExt.removeWhere(subscribtions, new IFunction1<Boolean, TSubscriptionItem>()
                            {
                                @Override
                                public Boolean invoke(TSubscriptionItem x)
                                        throws Exception
                                {
                                    if (x.getReceiverId().equals(responseReceiverId))
                                    {
                                        // If it is one of messages that should be unsubscribed then return
                                        // true indicating the item shall be removed.
                                        for (String aMessageType : messageTypes)
                                        {
                                            if (aMessageType.equals(x.getMessageTypeId()))
                                            {
                                                return true;
                                            }
                                        }
                                    }
                                    
                                    return false;
                                }
                        
                            });
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private Object mySubscribtionManipulatorLock = new Object();
    private HashSet<TSubscriptionItem> myMessageSubscribtions = new HashSet<TSubscriptionItem>();
    private HashSet<TSubscriptionItem> myRegExpSubscribtions = new HashSet<TSubscriptionItem>();
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

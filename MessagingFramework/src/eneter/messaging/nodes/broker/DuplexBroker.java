/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import java.util.*;
import java.util.regex.Pattern;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.nodes.channelwrapper.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;
import eneter.net.system.linq.internal.EnumerableExt;

class DuplexBroker implements IDuplexBroker
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
    
    public DuplexBroker(IMessagingSystemFactory localMessaging,
            IChannelWrapperFactory channelWrapperFactory,
            IDuplexTypedMessagesFactory typedRequestResponseFactory) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDuplexChannelUnwrapper = channelWrapperFactory.createDuplexChannelUnwrapper(localMessaging);
    
            myBrokerRequestReceiver = typedRequestResponseFactory.createDuplexTypedMessageReceiver(BrokerNotifyMessage.class, BrokerRequestMessage.class);
            myBrokerRequestReceiver.messageReceived().subscribe(myOnBrokerRequestReceivedHandler);
            myBrokerRequestReceiver.responseReceiverDisconnected().subscribe(myOnSubscriberDisconnectedHandler);
            
            IDuplexInputChannel aRequestReceiverInputChannel = localMessaging.createDuplexInputChannel("BrokerRequestChannel");
            myBrokerRequestReceiver.attachDuplexInputChannel(aRequestReceiverInputChannel);
            
            myBrokerMessagesReceiver = typedRequestResponseFactory.createDuplexTypedMessageReceiver(boolean.class, BrokerNotifyMessage.class);
            myBrokerMessagesReceiver.messageReceived().subscribe(myOnBrokerMessageReceivedHandler);
            
            IDuplexInputChannel aMessageReceiverInputChannel = localMessaging.createDuplexInputChannel("BrokerMessageChannel");
            myBrokerMessagesReceiver.attachDuplexInputChannel(aMessageReceiverInputChannel);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void attachDuplexInputChannel(IDuplexInputChannel duplexInputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myDuplexChannelUnwrapper.attachDuplexInputChannel(duplexInputChannel);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to attach duplex input channel '" + duplexInputChannel.getChannelId() + "'.", err);
                throw err;
            }

            myDuplexInputChannelId = duplexInputChannel.getChannelId();
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
            myDuplexInputChannelId = "";

            try
            {
                myDuplexChannelUnwrapper.detachDuplexInputChannel();
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "failed to detach duplex input channel.", err);
            }

            myDuplexChannelUnwrapper.responseReceiverDisconnected().unsubscribe(myOnSubscriberDisconnectedHandler);
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
            return myDuplexChannelUnwrapper.isDuplexInputChannelAttached();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public IDuplexInputChannel getAttachedDuplexInputChannel()
    { 
        return myDuplexChannelUnwrapper.getAttachedDuplexInputChannel();
    }
    

    @Override
    public void sendMessage(String messageTypeId, Object serializedMessage)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            BrokerNotifyMessage aNotifyMessage = new BrokerNotifyMessage(messageTypeId, serializedMessage);
            publish(aNotifyMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void subscribe(String messageType) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String[] aEventsToSubscribe = { messageType };
            subscribe(myLocalReceiverId, aEventsToSubscribe, myMessageSubscribtions);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void subscribe(String[] messageTypes) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            subscribe(myLocalReceiverId, messageTypes, myMessageSubscribtions);
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
    public void unsubscribe(String messageType) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String[] aEventsToUnsubscribe = { messageType };
            unsubscribe(myLocalReceiverId, aEventsToUnsubscribe, myMessageSubscribtions);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void unsubscribe(String[] messageTypes) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            unsubscribe(myLocalReceiverId, messageTypes, myMessageSubscribtions);
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
    
    private void onBrokerRequestReceived(Object sender, TypedRequestReceivedEventArgs<BrokerRequestMessage> e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (e.getReceivingError() != null)
            {
                EneterTrace.error(TracedObject() + "detected an error during receiving a request to subscribe a client.", e.getReceivingError());
                return;
            }

            synchronized (mySubscribtionManipulatorLock)
            {
                if (e.getRequestMessage().Request == EBrokerRequest.Subscribe)
                {
                    subscribe(e.getResponseReceiverId(), e.getRequestMessage().MessageTypes, myMessageSubscribtions);
                }
                else if (e.getRequestMessage().Request == EBrokerRequest.SubscribeRegExp)
                {
                    subscribe(e.getResponseReceiverId(), e.getRequestMessage().MessageTypes, myRegExpSubscribtions);
                }
                else if (e.getRequestMessage().Request == EBrokerRequest.Unsubscribe)
                {
                    unsubscribe(e.getResponseReceiverId(), e.getRequestMessage().MessageTypes, myMessageSubscribtions);
                }
                else if (e.getRequestMessage().Request == EBrokerRequest.UnsubscribeRegExp)
                {
                    unsubscribe(e.getResponseReceiverId(), e.getRequestMessage().MessageTypes, myRegExpSubscribtions);
                }
                else if (e.getRequestMessage().Request == EBrokerRequest.UnsubscribeAll)
                {
                    synchronized (mySubscribtionManipulatorLock)
                    {
                        unsubscribe(e.getResponseReceiverId(), null, myMessageSubscribtions);
                        unsubscribe(e.getResponseReceiverId(), null, myRegExpSubscribtions);
                    }
                }
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "detected exception when broker request received.", err);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onBrokerMessageReceived(Object sender, final TypedRequestReceivedEventArgs<BrokerNotifyMessage> e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (e.getReceivingError() != null)
            {
                EneterTrace.error(TracedObject() + "detected an error during receiving a message that should be forwarded to subscribed clients.", e.getReceivingError());
                return;
            }

            publish(e.getRequestMessage());
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "detected exception when broker message received.", err);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void publish(final BrokerNotifyMessage message) throws Exception
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
                            public Boolean invoke(TSubscriptionItem x)
                                    throws Exception
                            {
                                return x.getMessageTypeId().equals(message.MessageTypeId);
                            }
                        });
                
                Iterable<TSubscriptionItem> aRegExpSubscribers = EnumerableExt.where(myRegExpSubscribtions, new IFunction1<Boolean, TSubscriptionItem>()
                        {
                            @Override
                            public Boolean invoke(TSubscriptionItem x)
                                    throws Exception
                            {
                                EneterTrace aTrace = EneterTrace.entering();
                                try
                                {
                                    try
                                    {
                                        String aRegEx = x.getMessageTypeId();
                                        String aMessageType = message.MessageTypeId;
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
                                finally
                                {
                                    EneterTrace.leaving(aTrace);
                                }
                            }
                        });


                // Remove subscriptions with regular expresions causing exceptions.
                for (TSubscriptionItem x : anIncorrectRegExpCollector)
                {
                    myRegExpSubscribtions.remove(x);
                }
                
                // Notify subscribers
                sendNotifyMessages(message, aMessageSubscribers);
                
                // Notify subscribers subscribed via the regular expression.
                sendNotifyMessages(message, aRegExpSubscribers);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void sendNotifyMessages(BrokerNotifyMessage message, Iterable<TSubscriptionItem> subscribers)
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
                            BrokerMessageReceivedEventArgs anEvent = new BrokerMessageReceivedEventArgs(message.MessageTypeId, message.Message);
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
                    try
                    {
                        myBrokerRequestReceiver.sendResponseMessage(aSubscriber.getReceiverId(), message);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to send a message to the subscriber '" + aSubscriber.getReceiverId() + "'", err);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onSubscriberDisconnected(Object sender, ResponseReceiverEventArgs e)
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
    
    
    
    private IDuplexChannelUnwrapper myDuplexChannelUnwrapper;
    
    // Receives requests to subscribe or unsubscribe.
    private IDuplexTypedMessageReceiver<BrokerNotifyMessage, BrokerRequestMessage> myBrokerRequestReceiver;
    
    // Receive messages to be forwarded to subscribers.
    private IDuplexTypedMessageReceiver<Boolean, BrokerNotifyMessage> myBrokerMessagesReceiver;
    
    private Object mySubscribtionManipulatorLock = new Object();
    
    private HashSet<TSubscriptionItem> myMessageSubscribtions = new HashSet<TSubscriptionItem>();
    
    private HashSet<TSubscriptionItem> myRegExpSubscribtions = new HashSet<TSubscriptionItem>();
    
    private final String myLocalReceiverId = "Eneter.Broker.LocalReceiver";
    private String myDuplexInputChannelId = "";
    
    
    private EventImpl<BrokerMessageReceivedEventArgs> myBrokerMessageReceivedEvent = new EventImpl<BrokerMessageReceivedEventArgs>();
    
    
    private EventHandler<TypedRequestReceivedEventArgs<BrokerNotifyMessage>> myOnBrokerMessageReceivedHandler = new EventHandler<TypedRequestReceivedEventArgs<BrokerNotifyMessage>>()
            {
                @Override
                public void onEvent(Object sender, TypedRequestReceivedEventArgs<BrokerNotifyMessage> e)
                {
                    onBrokerMessageReceived(sender, e);
                }
            };
    
    private EventHandler<TypedRequestReceivedEventArgs<BrokerRequestMessage>> myOnBrokerRequestReceivedHandler = new EventHandler<TypedRequestReceivedEventArgs<BrokerRequestMessage>>()
    {
        @Override
        public void onEvent(Object sender, TypedRequestReceivedEventArgs<BrokerRequestMessage> e)
        {
            onBrokerRequestReceived(sender, e);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myOnSubscriberDisconnectedHandler = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onSubscriberDisconnected(sender, e);
        }
    };
    
    
    private String TracedObject()
    {
        return "The Broker atached to the duplex input channel '" + myDuplexInputChannelId + "' ";
    }

}

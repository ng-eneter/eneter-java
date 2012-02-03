/**
 * Project: Eneter.Messaging.Framework for Java
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import java.util.*;
import java.util.regex.Pattern;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.nodes.channelwrapper.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.HashSetExt;
import eneter.net.system.linq.EnumerableExt;

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
            catch (Error err)
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
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + "failed to detach duplex input channel.", err);
                throw err;
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
    
    private void onBrokerRequestReceived(Object sender, TypedRequestReceivedEventArgs<BrokerRequestMessage> e) throws Exception
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
                    unsubscribe(e.getResponseReceiverId(), null, myMessageSubscribtions);
                    unsubscribe(e.getResponseReceiverId(), null, myRegExpSubscribtions);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onBrokerMessageReceived(Object sender, final TypedRequestReceivedEventArgs<BrokerNotifyMessage> e) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (e.getReceivingError() != null)
            {
                EneterTrace.error(TracedObject() + "detected an error during receiving a message that should be forwarded to subscribed clients.", e.getReceivingError());
                return;
            }

            synchronized (mySubscribtionManipulatorLock)
            {
                final ArrayList<TSubscriptionItem> anIncorrectRegExpCollector = new ArrayList<TSubscriptionItem>();

                Iterable<TSubscriptionItem> aMessageSubscribers = EnumerableExt.where(myMessageSubscribtions, new IFunction1<Boolean, TSubscriptionItem>()
                        {
                            @Override
                            public Boolean invoke(TSubscriptionItem x)
                                    throws Exception
                            {
                                return x.getMessageTypeId().equals(e.getRequestMessage().MessageTypeId);
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
                                        String aMessageType = e.getRequestMessage().MessageTypeId;
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
                for (TSubscriptionItem aSubscriber : aMessageSubscribers)
                {
                    try
                    {
                        myBrokerRequestReceiver.sendResponseMessage(aSubscriber.getReceiverId(), e.getRequestMessage());
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to send a message to the subscriber '" + aSubscriber.getReceiverId() + "'", err);
                    }
                    catch (Error err)
                    {
                        EneterTrace.error(TracedObject() + "failed to send a message to the subscriber '" + aSubscriber.getReceiverId() + "'", err);
                        throw err;
                    }
                }
                
                // Notify subscribers subscribed via the regular expression.
                for (TSubscriptionItem aSubscriber : aRegExpSubscribers)
                {
                    try
                    {
                        myBrokerRequestReceiver.sendResponseMessage(aSubscriber.getReceiverId(), e.getRequestMessage());
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to send a message to the subscriber '" + aSubscriber.getReceiverId() + "'", err);
                    }
                    catch (Error err)
                    {
                        EneterTrace.error(TracedObject() + "failed to send a message to the subscriber '" + aSubscriber.getReceiverId() + "'", err);
                        throw err;
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onSubscriberDisconnected(Object sender, ResponseReceiverEventArgs e) throws Exception
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
            for (String aMessageType : messageTypes)
            {
                subscribtions.add(new TSubscriptionItem(aMessageType, responseReceiverId));
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void unsubscribe(final String responseReceiverId, String[] messageTypes, HashSet<TSubscriptionItem> subscribtions) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
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
                for (final String aMessageType : messageTypes)
                {
                    HashSetExt.removeWhere(subscribtions, new IFunction1<Boolean, TSubscriptionItem>()
                            {
                                @Override
                                public Boolean invoke(TSubscriptionItem x)
                                        throws Exception
                                {
                                    return x.getReceiverId().equals(responseReceiverId) && x.getMessageTypeId().equals(aMessageType);
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
    
    private String myDuplexInputChannelId = "";
    
    
    private EventHandler<TypedRequestReceivedEventArgs<BrokerNotifyMessage>> myOnBrokerMessageReceivedHandler = new EventHandler<TypedRequestReceivedEventArgs<BrokerNotifyMessage>>()
            {
                @Override
                public void invoke(Object sender, TypedRequestReceivedEventArgs<BrokerNotifyMessage> e)
                        throws Exception
                {
                    onBrokerMessageReceived(sender, e);
                }
            };
    
    private EventHandler<TypedRequestReceivedEventArgs<BrokerRequestMessage>> myOnBrokerRequestReceivedHandler = new EventHandler<TypedRequestReceivedEventArgs<BrokerRequestMessage>>()
    {
        @Override
        public void invoke(Object sender, TypedRequestReceivedEventArgs<BrokerRequestMessage> e)
                throws Exception
        {
            onBrokerRequestReceived(sender, e);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myOnSubscriberDisconnectedHandler = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void invoke(Object sender, ResponseReceiverEventArgs e)
                throws Exception
        {
            onSubscriberDisconnected(sender, e);
        }
    };
    
    
    private String TracedObject()
    {
        return "The Broker atached to the duplex input channel '" + myDuplexInputChannelId + "' ";
    }
}

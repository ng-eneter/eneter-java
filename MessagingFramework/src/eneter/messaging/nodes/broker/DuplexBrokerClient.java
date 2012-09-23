/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import java.util.regex.Pattern;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.nodes.channelwrapper.*;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;

class DuplexBrokerClient implements IDuplexBrokerClient
{
    public DuplexBrokerClient(IMessagingSystemFactory localMessaging,
            IChannelWrapperFactory channelWrapperFactory,
            IDuplexTypedMessagesFactory typedRequestResponseFactory) throws Exception
    {
        
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDuplexChannelWrapper = channelWrapperFactory.createDuplexChannelWrapper();
            
            myBrokerRequestSender = typedRequestResponseFactory.createDuplexTypedMessageSender(BrokerNotifyMessage.class, BrokerRequestMessage.class);
            myBrokerRequestSender.responseReceived().subscribe(myOnBrokerMessageReceivedHandler);
            
            IDuplexInputChannel aRequestInputChannel = localMessaging.createDuplexInputChannel("BrokerRequestChannel");
            IDuplexOutputChannel aRequestOutputChannel = localMessaging.createDuplexOutputChannel("BrokerRequestChannel");
            myDuplexChannelWrapper.attachDuplexInputChannel(aRequestInputChannel);
            myBrokerRequestSender.attachDuplexOutputChannel(aRequestOutputChannel);
            
            
            myBrokerMessagesSender = typedRequestResponseFactory.createDuplexTypedMessageSender(boolean.class, BrokerNotifyMessage.class);
            
            IDuplexInputChannel aMessageInputChannel = localMessaging.createDuplexInputChannel("BrokerMessageChannel");
            IDuplexOutputChannel aMessageOutputChannel = localMessaging.createDuplexOutputChannel("BrokerMessageChannel");
            myDuplexChannelWrapper.attachDuplexInputChannel(aMessageInputChannel);
            myBrokerMessagesSender.attachDuplexOutputChannel(aMessageOutputChannel);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public Event<BrokerMessageReceivedEventArgs> brokerMessageReceived()
    {
        return myBrokerMessageReceivedEventImpl.getApi();
    }
    
    
    @Override
    public void attachDuplexOutputChannel(IDuplexOutputChannel duplexOutputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myDuplexChannelWrapper.attachDuplexOutputChannel(duplexOutputChannel);
                myDuplexOutputChannelId = duplexOutputChannel.getChannelId();
            }
            catch (Exception err)
            {
                String aChannelId = (duplexOutputChannel != null) ? duplexOutputChannel.getChannelId() : "";
                EneterTrace.error(TracedObject() + "failed to attach the duplex output channel '" + aChannelId + "' and open connection.", err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachDuplexOutputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDuplexChannelWrapper.detachDuplexOutputChannel();
            myDuplexOutputChannelId = "";
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isDuplexOutputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myDuplexChannelWrapper.isDuplexOutputChannelAttached();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel getAttachedDuplexOutputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myDuplexChannelWrapper.getAttachedDuplexOutputChannel();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void sendMessage(String messageTypeId, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                BrokerNotifyMessage aBrokerMessage = new BrokerNotifyMessage(messageTypeId, message);
                myBrokerMessagesSender.sendRequestMessage(aBrokerMessage);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to send the message to the broker.", err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void subscribe(String eventIds) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String[] aMessageType = { eventIds };
            subscribe(aMessageType);
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
            if (eventIds == null)
            {
                String anErrorMessage = TracedObject() + "cannot subscribe to null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalArgumentException(anErrorMessage);
            }
            
            // Check input items.
            for (String anInputItem : eventIds)
            {
                if (StringExt.isNullOrEmpty(anInputItem))
                {
                    String anErrorMessage = TracedObject() + "cannot subscribe to null.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalArgumentException(anErrorMessage);
                }
            }
            
            sendRequest(EBrokerRequest.Subscribe, eventIds);
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
            String[] aRegularExpression = { regularExpression };
            subscribeRegExp(aRegularExpression);
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
            // Check if the client has a correct regular expression.
            // If not, then the exception will be thrown here on the client side and not in the broker during the evaluation.
            for (String aRegExpression : regularExpressions)
            {
                try
                {
                    Pattern.matches(aRegExpression, "");
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to subscribe the regular expression because the regular expression '"
                        + aRegExpression + "' is incorrect.", err);
                    throw err;
                }
            }

            sendRequest(EBrokerRequest.SubscribeRegExp, regularExpressions);
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
            String[] aMessageType = { messageType };
            sendRequest(EBrokerRequest.Unsubscribe, aMessageType);
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
            sendRequest(EBrokerRequest.Unsubscribe, messageTypes);
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
            String[] aRegularExpression = { regularExpression };
            sendRequest(EBrokerRequest.UnsubscribeRegExp, aRegularExpression);
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
            sendRequest(EBrokerRequest.UnsubscribeRegExp, regularExpressions);
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
            // Unsubscribe from all messages.
            String[] anEmpty = new String[0];
            sendRequest(EBrokerRequest.UnsubscribeAll, anEmpty);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    private void onBrokerMessageReceived(Object sender, TypedResponseReceivedEventArgs<BrokerNotifyMessage> e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myBrokerMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    BrokerMessageReceivedEventArgs anEvent = null;

                    if (e.getReceivingError() == null)
                    {
                        anEvent = new BrokerMessageReceivedEventArgs(e.getResponseMessage().MessageTypeId, e.getResponseMessage().Message);
                    }
                    else
                    {
                        anEvent = new BrokerMessageReceivedEventArgs(e.getReceivingError());
                    }

                    myBrokerMessageReceivedEventImpl.raise(this, anEvent);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void sendRequest(EBrokerRequest request, String[] messageTypes) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                BrokerRequestMessage aBrokerRequestMessage = new BrokerRequestMessage(request, messageTypes);
                myBrokerRequestSender.sendRequestMessage(aBrokerRequestMessage);
            }
            catch (Exception err)
            {
                String anError = (request == EBrokerRequest.Subscribe || request == EBrokerRequest.SubscribeRegExp) ?
                    TracedObject() + "failed to subscribe in the Broker." :
                    TracedObject() + "failed to unsubscribe in the Broker.";

                EneterTrace.error(anError, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    
    private EventImpl<BrokerMessageReceivedEventArgs> myBrokerMessageReceivedEventImpl = new EventImpl<BrokerMessageReceivedEventArgs>();
    
    
    private IDuplexChannelWrapper myDuplexChannelWrapper;

    private IDuplexTypedMessageSender<BrokerNotifyMessage, BrokerRequestMessage> myBrokerRequestSender;
    private IDuplexTypedMessageSender<Boolean, BrokerNotifyMessage> myBrokerMessagesSender;
    
    private String myDuplexOutputChannelId = "";
   
    
    private EventHandler<TypedResponseReceivedEventArgs<BrokerNotifyMessage>> myOnBrokerMessageReceivedHandler = new EventHandler<TypedResponseReceivedEventArgs<BrokerNotifyMessage>>()
    {
        @Override
        public void onEvent(Object sender, TypedResponseReceivedEventArgs<BrokerNotifyMessage> e)
        {
            onBrokerMessageReceived(sender, e);
        }
    };
    
   
    private String TracedObject()
    {
        return "The DuplexBrokerClient atached to the duplex output channel '" + myDuplexOutputChannelId + "' ";
    }

}

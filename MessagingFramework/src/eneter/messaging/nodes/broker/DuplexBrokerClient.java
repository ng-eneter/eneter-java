/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import java.util.regex.Pattern;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexOutputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;

class DuplexBrokerClient extends AttachableDuplexOutputChannelBase implements IDuplexBrokerClient
{
    public DuplexBrokerClient(ISerializer serializer)
    {
        
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public Event<BrokerMessageReceivedEventArgs> brokerMessageReceived()
    {
        return myBrokerMessageReceivedEvent.getApi();
    }
    
    

    @Override
    public void sendMessage(String messageTypeId, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                BrokerMessage aBrokerMessage = new BrokerMessage(messageTypeId, message);
                send(aBrokerMessage);
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
            
            send(EBrokerRequest.Subscribe, eventIds);
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
            if (regularExpressions == null)
            {
                String anErrorMessage = TracedObject() + "cannot subscribe to null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalArgumentException(anErrorMessage);
            }
            
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

            send(EBrokerRequest.SubscribeRegExp, regularExpressions);
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
            send(EBrokerRequest.Unsubscribe, aMessageType);
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
            send(EBrokerRequest.Unsubscribe, messageTypes);
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
            send(EBrokerRequest.UnsubscribeRegExp, aRegularExpression);
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
            send(EBrokerRequest.UnsubscribeRegExp, regularExpressions);
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
            send(EBrokerRequest.UnsubscribeAll, anEmpty);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    protected void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (!myBrokerMessageReceivedEvent.isSubscribed())
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
                return;
            }

            BrokerMessageReceivedEventArgs anEvent = null;
            try
            {
                BrokerMessage aMessage = mySerializer.deserialize(e.getMessage(), BrokerMessage.class);
                anEvent = new BrokerMessageReceivedEventArgs(aMessage.MessageTypes[0], aMessage.Message);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "failed to deserialize the request message.", err);
                anEvent = new BrokerMessageReceivedEventArgs(err);
            }

            try
            {
                myBrokerMessageReceivedEvent.raise(this, anEvent);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void send(EBrokerRequest request, String[] messageTypes) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            BrokerMessage aBrokerMessage = new BrokerMessage(request, messageTypes);
            send(aBrokerMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void send(BrokerMessage message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (getAttachedDuplexOutputChannel() == null)
            {
                String anError = TracedObject() + "failed to send the message because it is not attached to any duplex output channel.";
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }

            try
            {
                Object aSerializedMessage = mySerializer.serialize(message, BrokerMessage.class);
                getAttachedDuplexOutputChannel().sendMessage(aSerializedMessage);
            }
            catch (Exception err)
            {
                String anError = TracedObject() + "failed to send a message to the Broker.";
                EneterTrace.error(anError, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private EventImpl<BrokerMessageReceivedEventArgs> myBrokerMessageReceivedEvent = new EventImpl<BrokerMessageReceivedEventArgs>();
    
    private ISerializer mySerializer;    
    private String myDuplexOutputChannelId = "";
   
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " '" + myDuplexOutputChannelId + "' ";
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright � 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.diagnostic.*;
import eneter.messaging.infrastructure.attachable.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelMessageEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.*;


class DuplexStringMessageReceiver extends AttachableDuplexInputChannelBase
                                  implements IDuplexStringMessageReceiver
{
    @Override
    public Event<StringRequestReceivedEventArgs> requestReceived()
    {
        return myRequestReceivedEventApi;
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEventApi;
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEventApi;
    }

    @Override
    public void sendResponseMessage(String responseReceiverId,
            String responseMessage) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (getAttachedDuplexInputChannel() == null)
            {
                String anError = TracedObject() + "failed to send the response message because it is not attached to any duplex input channel.";
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }

            try
            {
                getAttachedDuplexInputChannel().sendResponseMessage(responseReceiverId, responseMessage);
            }
            catch (Exception err)
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

    @Override
    protected void onMessageReceived(Object sender,
            DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myRequestReceivedEventImpl.isEmpty())
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
                return;
            }

            if (e.getMessage() instanceof String == false)
            {
                String anErrorMessage = TracedObject() + "failed to receive the request message because the message is not string.";
                EneterTrace.error(anErrorMessage);
                return;
            }

            try
            {
                myRequestReceivedEventImpl.update(this, new StringRequestReceivedEventArgs((String)e.getMessage(), e.getResponseReceiverId()));
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

    @Override
    protected void onResponseReceiverConnected(Object sender,
            ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverConnectedEventImpl.isEmpty() == false)
            {
                try
                {
                    myResponseReceiverConnectedEventImpl.update(this, e);
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

    @Override
    protected void onResponseReceiverDisconnected(Object sender,
            ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverDisconnectedEventImpl.isEmpty() == false)
            {
                try
                {
                    myResponseReceiverDisconnectedEventImpl.update(this, e);
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

    
    
    private EventImpl<StringRequestReceivedEventArgs> myRequestReceivedEventImpl = new EventImpl<StringRequestReceivedEventArgs>();
    private Event<StringRequestReceivedEventArgs> myRequestReceivedEventApi = new Event<StringRequestReceivedEventArgs>(myRequestReceivedEventImpl);
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private Event<ResponseReceiverEventArgs> myResponseReceiverConnectedEventApi = new Event<ResponseReceiverEventArgs>(myResponseReceiverConnectedEventImpl);
    
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private Event<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventApi = new Event<ResponseReceiverEventArgs>(myResponseReceiverDisconnectedEventImpl);
    
    
    
    @Override
    protected String TracedObject()
    {
        String aDuplexInputChannelId = (getAttachedDuplexInputChannel() != null) ? getAttachedDuplexInputChannel().getChannelId() : "";
        return "The StringResponser atached to the duplex input channel '" + aDuplexInputChannelId + "' ";
    }
}

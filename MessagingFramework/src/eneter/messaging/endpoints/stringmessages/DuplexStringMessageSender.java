/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright � 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.diagnostic.*;
import eneter.messaging.infrastructure.attachable.AttachableDuplexOutputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelMessageEventArgs;
import eneter.net.system.*;

class DuplexStringMessageSender extends AttachableDuplexOutputChannelBase
                                implements IDuplexStringMessageSender
{

    @Override
    public Event<StringResponseReceivedEventArgs> responseReceived()
    {
        return myResponseReceivedEventApi;
    }

    @Override
    public void sendMessage(String message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (getAttachedDuplexOutputChannel() == null)
            {
                String anError = TracedObject() + "failed to send the request message because it is not attached to any duplex output channel.";
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }

            try
            {
                getAttachedDuplexOutputChannel().sendMessage(message);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void onResponseMessageReceived(Object sender,
            DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceivedEventImpl.isEmpty())
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
                return;
            }

            if (e.getMessage() instanceof String == false)
            {
                String anErrorMessage = TracedObject() + "failed to receive the response message because the message is not string.";
                EneterTrace.error(anErrorMessage);
                return;
            }

            try
            {
                myResponseReceivedEventImpl.update(this, new StringResponseReceivedEventArgs((String)e.getMessage()));
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
    protected String TracedObject()
    {
        String aDuplexOutputChannelId = (getAttachedDuplexOutputChannel() != null) ? getAttachedDuplexOutputChannel().getChannelId() : "";
        return "The DuplexStringMessageSender atached to the duplex output channel '" + aDuplexOutputChannelId + "' ";
    }

    
    private EventImpl<StringResponseReceivedEventArgs> myResponseReceivedEventImpl = new EventImpl<StringResponseReceivedEventArgs>();
    private Event<StringResponseReceivedEventArgs> myResponseReceivedEventApi = new Event<StringResponseReceivedEventArgs>(myResponseReceivedEventImpl);
}
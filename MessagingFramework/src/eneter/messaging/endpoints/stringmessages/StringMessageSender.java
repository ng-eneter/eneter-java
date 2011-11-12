/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.diagnostic.*;
import eneter.messaging.infrastructure.attachable.AttachableOutputChannelBase;

class StringMessageSender extends AttachableOutputChannelBase 
                                       implements IStringMessageSender
{
    @Override
    public void sendMessage(String message)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (getAttachedOutputChannel() == null)
            {
                String anError = "The StringMessageSender failed to send the message because it is not attached to any output channel.";
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }

            try
            {
                getAttachedOutputChannel().sendMessage(message);
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

    
    private String TracedObject()
    {
        String anOutputChannelId = (getAttachedOutputChannel() != null) ? getAttachedOutputChannel().getChannelId() : "";
        return "The StringMessageSender attached to the output channel '" + anOutputChannelId + "' ";
    }
}

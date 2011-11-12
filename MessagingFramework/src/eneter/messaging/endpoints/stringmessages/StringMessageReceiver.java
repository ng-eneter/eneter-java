/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.diagnostic.*;
import eneter.messaging.infrastructure.attachable.AttachableInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.ChannelMessageEventArgs;
import eneter.net.system.*;

class StringMessageReceiver extends AttachableInputChannelBase
                                         implements IStringMessageReceiver
{

    @Override
    public Event<StringMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventApi;
    }

    @Override
    protected void onMessageReceived(Object sender, ChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (!myMessageReceivedEventImpl.isEmpty())
            {
                if (e.getMessage() instanceof String)
                {
                    try
                    {
                        myMessageReceivedEventImpl.update(this, new StringMessageEventArgs((String)e.getMessage()));
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    }
                }
                else
                {
                    EneterTrace.error(TracedObject() + "received the message that was not type of string.");
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
    
    
    private EventImpl<StringMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<StringMessageEventArgs>();
    private Event<StringMessageEventArgs> myMessageReceivedEventApi = new Event<StringMessageEventArgs>(myMessageReceivedEventImpl);
    
    
    private String TracedObject()
    {
        String anInputChannelId = (getAttachedInputChannel() != null) ? getAttachedInputChannel().getChannelId() : "";
        return "The StringMessageReceiver atached to the input channel '" + anInputChannelId + "' ";
    }

}

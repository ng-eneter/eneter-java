/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase;

import java.security.InvalidParameterException;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;

public class SimpleOutputChannel implements IOutputChannel
{
    public SimpleOutputChannel(String channelId, IMessagingSystemBase messagingSystem)
    {
        if (channelId == null || channelId == "")
        {
            EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
            throw new InvalidParameterException(ErrorHandler.NullOrEmptyChannelId);
        }
        
        myChannelId = channelId;
        myMessagingSystem = messagingSystem;
    }
    
    public String getChannelId()
    {
        return myChannelId;
    }

    public void sendMessage(Object message)
        throws Exception
    {
        try
        {
            myMessagingSystem.sendMessage(myChannelId, message);
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
            throw err;
        }
        catch (Error err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
            throw err;
        }
    }
    
    private IMessagingSystemBase myMessagingSystem;
    private String myChannelId;
    
    private String TracedObject()
    {
        return "The output channel '" + myChannelId + "' ";
    }
}

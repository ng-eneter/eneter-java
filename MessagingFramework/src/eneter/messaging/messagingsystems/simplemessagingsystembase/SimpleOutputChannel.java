/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase;

import java.security.InvalidParameterException;

import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;

public class SimpleOutputChannel implements IOutputChannel
{
    public SimpleOutputChannel(String channelId, IMessagingSystemBase messagingSystem)
    {
        if (channelId == null || channelId == "")
        {
            // ??? Trace error
            throw new InvalidParameterException("Input parameter channelId is null or empty string.");
        }
        
        myChannelId = channelId;
        myMessagingSystem = messagingSystem;
    }
    
    public String getChannelId()
    {
        return myChannelId;
    }

    public void sendMessage(Object message)
    {
        try
        {
            myMessagingSystem.sendMessage(myChannelId, message);
        }
        catch (RuntimeException err)
        {
            // ??? Trace error.
            throw err;
        }
    }
    
    private IMessagingSystemBase myMessagingSystem;
    private String myChannelId;
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.security.InvalidParameterException;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;
import eneter.net.system.internal.StringExt;

public class SimpleOutputChannel implements IOutputChannel
{
    public SimpleOutputChannel(String channelId, IMessagingSystemBase messagingSystem, IProtocolFormatter<?> protocolFormatter)
    {
        if (StringExt.isNullOrEmpty(channelId))
        {
            EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
            throw new InvalidParameterException(ErrorHandler.NullOrEmptyChannelId);
        }
        
        myChannelId = channelId;
        myMessagingSystem = messagingSystem;
        myProtocolFormatter = protocolFormatter;
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
            // Encode the message according the protocol.
            Object anEncodedMessage = myProtocolFormatter.encodeMessage("", message);
            
            myMessagingSystem.sendMessage(myChannelId, anEncodedMessage);
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
            throw err;
        }
    }
    
    private IMessagingSystemBase myMessagingSystem;
    private String myChannelId;
    private IProtocolFormatter<?> myProtocolFormatter;
   
    
    private String TracedObject()
    {
        return "The output channel '" + myChannelId + "' ";
    }
}

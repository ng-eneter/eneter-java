/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;

public class MessageContext
{
    public MessageContext(ProtocolMessage message, String senderAddress)
    {
        myMessage = message;
        mySenderAddress = senderAddress;
    }

    public ProtocolMessage getProtocolMessage()
    {
        return myMessage;
    }
    
    public String getSenderAddress()
    {
        return mySenderAddress;
    }
    
    
    private ProtocolMessage myMessage;
    private String mySenderAddress;
}

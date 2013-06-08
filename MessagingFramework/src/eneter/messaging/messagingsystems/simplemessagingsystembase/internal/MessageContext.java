/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

public class MessageContext
{
    public MessageContext(Object message, String senderAddress, ISender responseSender)
    {
        myMessage = message;
        mySenderAddress = senderAddress;
        myResponseSender = responseSender;
    }

    public Object getMessage()
    {
        return myMessage;
    }
    
    public String getSenderAddress()
    {
        return mySenderAddress;
    }
    
    public ISender getResponseSender()
    {
        return myResponseSender;
    }
    
    public Object myMessage;
    public String mySenderAddress;
    public ISender myResponseSender;
}

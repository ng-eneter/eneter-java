/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit.internal;


/**
 * Type of the message (if it is ping or a data message).
 */
public enum MonitorChannelMessageType
{
    /**
     * Indicates, it is the ping message or ping response.
     */
    Ping(10),
    
    /**
     * Indicates, it is a message or a response message containing data. 
     */
    Message(20);
    
    public int geValue()
    {
        return myValue;
    }
    
    public static MonitorChannelMessageType fromInt(int i)
    {
        switch (i)
        {
            case 10: return Ping;
            case 20: return Message;
        }
        return null;
    }
    
    private MonitorChannelMessageType(int value)
    {
        myValue = value;
    }

    private final int myValue;
}

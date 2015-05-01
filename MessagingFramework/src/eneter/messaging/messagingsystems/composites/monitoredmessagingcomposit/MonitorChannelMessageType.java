/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;


/**
 * Type of the message.
 */
public enum MonitorChannelMessageType
{
    /**
     * Indicates, it is the ping message.
     */
    Ping(10),
    
    /**
     * Indicates, it is a regular data message between output and input channel.  
     */
    Message(20);
    
    /**
     * Converts enum to integer value.
     * @return integer value of the enum.
     */
    public int geValue()
    {
        return myValue;
    }
    
    /**
     * Converts integer value into the enum.
     * @param i integer value
     * @return enum
     */
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

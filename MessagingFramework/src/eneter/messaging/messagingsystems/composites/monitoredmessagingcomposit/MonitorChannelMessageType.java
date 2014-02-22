/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

/**
 * Type of the message (if it is ping or a data message).
 */
public enum MonitorChannelMessageType
{
    /**
     * Indicates, it is the ping message or ping response.
     */
    Ping,
    
    /**
     * Indicates, it is a message or a response message containing data. 
     */
    Message
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

/**
 * Type of the message sent by the monitor duplex output channel or monitor dupolex input channel.
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

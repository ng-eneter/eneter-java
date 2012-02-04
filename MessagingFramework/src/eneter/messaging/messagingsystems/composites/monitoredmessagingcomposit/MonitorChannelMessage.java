/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.io.Serializable;

/**
 * The message used by the monitor duplex output channel or
 * monitor duplex input channel for the communication.
 */
public class MonitorChannelMessage implements Serializable
{
    /**
     * Constructs the message. This constructor is used by the Xml serializer for the deserialization.
     */
    public MonitorChannelMessage()
    {
    }
    
    /**
     * Constructs the message from specified parameters.
     * 
     * @param messageType type of the message, ping or regular message
     * @param messageContent message content, in case of ping this parameter is not used
     */
    public MonitorChannelMessage(MonitorChannelMessageType messageType, Object messageContent)
    {
        MessageType = messageType;
        MessageContent = messageContent;
    }
    
    /**
     * Type of the message. Ping or regular message.
     */
    public MonitorChannelMessageType MessageType;
    
    /**
     * Message. In case of the 'ping', this property is null.
     */
    public Object MessageContent;

    private static final long serialVersionUID = -1411932226587371691L;
}

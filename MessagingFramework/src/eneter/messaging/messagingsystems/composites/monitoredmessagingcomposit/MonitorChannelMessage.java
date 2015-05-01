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
 * Internal message used for the communication between output and input channels in monitored messaging.
 */
public class MonitorChannelMessage implements Serializable

{
    /**
     * Constructs the message.
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
     * Serialized data message. In case of 'ping', this property is null.
     */
    public Object MessageContent;
    
    private static final long serialVersionUID = -1411932226587371691L;

}

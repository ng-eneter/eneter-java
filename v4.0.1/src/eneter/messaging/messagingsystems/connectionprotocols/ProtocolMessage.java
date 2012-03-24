/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.connectionprotocols;

/**
 * Represents decoded low-level protocol message.
 * 
 * The low-level messages are for the communication between channels. To establish or close connection, etc.
 */
public class ProtocolMessage
{
    /**
     * Default constructor.
     */
    public ProtocolMessage()
    {
    }

    /**
     * Constructs the protocol message from the given parameters.
     * 
     * @param messageType type of the message
     * @param responseReceiverId client id
     * @param message message content
     */
    public ProtocolMessage(EProtocolMessageType messageType, String responseReceiverId, Object message)
    {
        MessageType = messageType;
        ResponseReceiverId = responseReceiverId;
        Message = message;
    }

    /**
     * Type of the message.
     */
    public EProtocolMessageType MessageType;
    
    /**
     * Client id.
     */
    public String ResponseReceiverId;
    
    /**
     * The content of the message or response message.
     */
    public Object Message;
}

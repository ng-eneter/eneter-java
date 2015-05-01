/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.connectionprotocols;

/**
 * Message decoded by the protocol formatter.
 * 
 * The protocol formatter is used for the internal communication between output and input channel.
 * When the channel receives a message it uses the protocol formatter to figure out if is is 'Open Connection',
 * 'Close Connection' or 'Data Message'.<br/>
 * Protocol formatter decodes the message and returns ProtocolMessage.
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
     * If message type is MessageReceived the it contains the serialized message data.
     * Otherwise it is null.
     */
    public Object Message;
}

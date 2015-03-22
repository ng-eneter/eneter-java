/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.connectionprotocols;

import java.io.*;


/**
 * Declares functionality to encode and decode messages used for the communication between channels. 
 *
 */
public interface IProtocolFormatter
{
    /**
     * Encodes the open connection request message.
     * 
     * The message is used by the duplex output channel to open the connection with the duplex input channel.
     * 
     * @param responseReceiverId id of the client opening the connection.
     * @return encoded message
     * @throws Exception
     */
    Object encodeOpenConnectionMessage(String responseReceiverId) throws Exception;
    
    /**
     * Encodes the open connection request message to the stream.
     * 
     * @param responseReceiverId id of the client opening the connection.
     * @param outputSream output where the encoded message is written
     * @throws Exception
     */
    void encodeOpenConnectionMessage(String responseReceiverId, OutputStream outputSream) throws Exception;

    /**
     * Encodes the close connecion request message.
     * 
     * The message is used by the duplex output channel or duplex input channel to close the connection.
     * 
     * @param responseReceiverId id of the client that wants to disconnect or that will be disconnected
     * @return encoded message
     * @throws Exception
     */
    Object encodeCloseConnectionMessage(String responseReceiverId) throws Exception;
    
    /**
     * Encodes the close connecion request message to the stream.
     * 
     * @param responseReceiverId id of the client that wants to disconnect or that will be disconnected
     * @param outputSream output where the encoded message is written
     * @throws Exception
     */
    void encodeCloseConnectionMessage(String responseReceiverId, OutputStream outputSream) throws Exception;

    /**
     * Encodes a message or a response message.
     * 
     * The message is used by output channel or duplex output channel to send messages or
     * by duplex input channel to send response messages.
     * 
     * @param responseReceiverId client id. It is empty string in case of output channel.
     * @param message serialized message to be sent.
     * @return encoded message
     * @throws Exception
     */
    Object encodeMessage(String responseReceiverId, Object message) throws Exception;
    
    /**
     * Encodes a message or a response message to the stream.
     * 
     * @param responseReceiverId id of the client that wants to send the message. It is empty string if the response message is sent.
     * @param message serialized message to be sent.
     * @param outputSream output where the encoded message is written
     * @throws Exception
     */
    void encodeMessage(String responseReceiverId, Object message, OutputStream outputSream) throws Exception;

    /**
     * Decodes message from the stream.
     * 
     * @param readStream stream to be read
     * @return decoded message
     */
    ProtocolMessage decodeMessage(InputStream readStream);

    /**
     * Decodes message from the given object.
     * 
     * @param readMessage reference to the object.
     * @return decoded message
     */
    ProtocolMessage decodeMessage(Object readMessage);
}

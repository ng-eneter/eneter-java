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
 * Declares functionality to encode/decode messages used for the communication between channels. 
 *
 */
public interface IProtocolFormatter
{
    /**
     * Encodes the open connection request message.
     * 
     * The message is used by the output channel to open the connection with the input channel.
     * If the open connection message is not used it can return null. 
     * 
     * @param responseReceiverId id of the client opening the connection.
     * @return encoded open connection message.
     * @throws Exception
     */
    Object encodeOpenConnectionMessage(String responseReceiverId) throws Exception;
    
    /**
     * Encodes the open connection request message to the stream.
     * 
     * The message is used by the output channel to open the connection with the input channel.<br/>
     * If the open connection message is not used it can just return without writing to the stream.
     * 
     * @param responseReceiverId id of the client opening the connection.
     * @param outputSream output where the encoded open connection message is written
     * @throws Exception
     */
    void encodeOpenConnectionMessage(String responseReceiverId, OutputStream outputSream) throws Exception;

    /**
     * Encodes the close connection request message.
     * 
     * The message is used by the output channel to close the connection with the input channel.
     * It is also used by input channel when it disconnects the output channel.<br/>
     * If the close connection message is not used it can return null.
     * 
     * @param responseReceiverId id of the client that wants to disconnect or that will be disconnected
     * @return encoded close connection message
     * @throws Exception
     */
    Object encodeCloseConnectionMessage(String responseReceiverId) throws Exception;
    
    /**
     * Encodes the close connection request message to the stream.
     * 
     * The message is used by the output channel to close the connection with the input channel.
     * It is also used by input channel when it disconnects the output channel.<br/>
     * If the close connection message is not used it can just return without writing to the stream.
     * 
     * @param responseReceiverId id of the client that wants to disconnect or that will be disconnected
     * @param outputSream output where the encoded close connection message is written
     * @throws Exception
     */
    void encodeCloseConnectionMessage(String responseReceiverId, OutputStream outputSream) throws Exception;

    /**
     * Encodes the data message.
     * 
     * The message is used by the output as well as input channel to send the data message.
     * 
     * @param responseReceiverId client id.
     * @param message serialized message to be sent.
     * @return encoded data message
     * @throws Exception
     */
    Object encodeMessage(String responseReceiverId, Object message) throws Exception;
    
    /**
     * Encodes a message or a response message to the stream.
     * 
     * The message is used by the output as well as input channel to send the data message.
     * 
     * @param responseReceiverId id of the client that wants to send the message.
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

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.connectionprotocols;

import java.io.*;

import eneter.messaging.dataprocessing.serializing.internal.EncoderDecoder;
import eneter.messaging.diagnostic.EneterTrace;

/**
 * Simple and very fast encoding/decoding for TCP, WebSocket and multicast/broadcast UDP.
 * 
 * This protocol formatter is stripped to the bare minimum. It does not encode OpenConnection and CloseConnection
 * messages but it encodes only data messages.<br/>
 * Therefore it can be used only with communication protocols which have own mechanisms to open and close the
 * communication (TCP and WebSockets) or opening and closing the connection is not needed (multicast/broadcast UDP).<br/> 
 * <br/>
 * The simplicity of this formatter provides a high performance and easy portability to various platforms allowing so
 * to communicate with Eneter even without having the Eneter framework.<br/>
 * Here is the list of limitation when using this protocol formatter:
 * <ul>
 * <li>It can be used only with TCP, WebSockets or multicast/broadcast UDP.</li>
 * <li>It cannot be used if automatic reconnect is needed. It means it cannot be used in buffered messaging.</li>
 * </ul>
 * <br/>
 * Here is how this formatter encodes messages between channels:
 * <b>Open connection message:</b><br/>
 * N.A. - the open connection message is not used. The connection is considered open when the socket is open.<br/>
 * <br/>
 * <b>Close connection message:</b><br/>
 * N.A. = the close connection message is not used. The connection is considered closed then the socket is closed.<br/>
 * <br/>
 * <b>Data message:</b><br/>
 * 1 byte - type of data: 10 string in UTF8, 40 bytes<br/>
 * 4 bytes - length: 32 bit integer indicating the size (in bytes) of message data.<br/>
 * x bytes - message data<br/>
 * <br/>
 * The 32 bit integer indicating the length of message data is encoded as little endian byte default.
 * If big endian is needed it is possible to specify it in the constructor.<br/>
 * <br/>
 * The following example shows how to use TCP messaging with EasyProtocolFormatter:<br/>
 * <pre>
 * {@code
 * // Instantiate protocol formatter. 
 * IProtocolFormatter aFormatter = new EasyProtocolFormatter();
 * 
 * // Provide the protocol formatter into the messaging constructor.
 * // Note: It can be only TCP or WebSocket messaging.
 * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory(aFormatter);
 * ...
 * // Then use can use the messaging to create channels.
 * IDuplexOutputChannel anOutputChannel = aMessaging.CreateDuplexOutputChannel("tcp://127.0.0.1:8084/");
 * ...
 * }
 * </pre>
 *  
 *
 */
public class EasyProtocolFormatter implements IProtocolFormatter
{
    /**
     * Constructs the protocol formatter with default little endian encoding.
     */
    public EasyProtocolFormatter()
    {
        this(true);
    }

    /**
     * Constructs the protocol formatter with specified endianess.
     * 
     * @param isLittleEndian true - little endian, false - big endian.
     */
    public EasyProtocolFormatter(boolean isLittleEndian)
    {
        myIsLittleEndian = isLittleEndian;
    }

    /**
     * Returns null.
     * The open connection message is not used. Therefore it returns null.
     */
    @Override
    public Object encodeOpenConnectionMessage(String responseReceiverId)
            throws Exception
    {
        // An explicit open connection message is not supported.
        return null;
    }
    
    /**
     * Does nothing.
     * The open connection message is not used. Therefore it does not write any data to the provided output stream.
     */
    @Override
    public void encodeOpenConnectionMessage(String responseReceiverId, OutputStream outputSream) throws Exception
    {
        // An explicit open connection message is not supported therefore write nothing into the sream.
    }
    
    /**
     * Returns null.
     * The close connection message is not used. Therefore it returns null.
     */
    @Override
    public Object encodeCloseConnectionMessage(String responseReceiverId)
            throws Exception
    {
        // An explicit close connection message is not supported.
        return null;
    }
    
    /**
     * Does nothing.
     * The close connection message is not used. Therefore it does not write any data to the provided output stream.
     */
    @Override
    public void encodeCloseConnectionMessage(String responseReceiverId, OutputStream outputSream) throws Exception
    {
        // An explicit close connection message is not supported therefore write nothing into the stream.
    }
    
    @Override
    public Object encodeMessage(String responseReceiverId, Object message)
            throws Exception
    {
        ByteArrayOutputStream aBuffer = new ByteArrayOutputStream();
        encodeMessage(responseReceiverId, message, aBuffer);
        
        return aBuffer.toByteArray();
    }
    
    @Override
    public void encodeMessage(String responseReceiverId, Object message, OutputStream outputSream) throws Exception
    {
        DataOutputStream aWriter = new DataOutputStream(outputSream);
        myEncoderDecoder.write(aWriter, message, myIsLittleEndian);
    }
    
    @Override
    public ProtocolMessage decodeMessage(Object readMessage)
    {
        ByteArrayInputStream aMemoryStream = new ByteArrayInputStream((byte[])readMessage);
        return decodeMessage(aMemoryStream);
    }
    
    @Override
    public ProtocolMessage decodeMessage(InputStream readStream)
    {
        try
        {
            DataInputStream aReader = new DataInputStream(readStream);
            Object aMessageData = myEncoderDecoder.read(aReader, myIsLittleEndian);

            ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.MessageReceived, "", aMessageData);
            return aProtocolMessage;
        }
        catch (IOException err)
        {
            // End of the stream. (e.g. underlying socket was closed)
            return null;
        }
        catch (Exception err)
        {
            EneterTrace.warning("Failed to decode the message.", err);
            
            return null;
        }
    }
    
    
    
    private boolean myIsLittleEndian;
    private EncoderDecoder myEncoderDecoder = new EncoderDecoder();
}

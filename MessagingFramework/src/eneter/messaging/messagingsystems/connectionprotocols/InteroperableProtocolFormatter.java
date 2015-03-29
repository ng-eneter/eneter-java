package eneter.messaging.messagingsystems.connectionprotocols;

import java.io.*;

import eneter.messaging.dataprocessing.serializing.internal.EncoderDecoder;
import eneter.messaging.diagnostic.EneterTrace;

public class InteroperableProtocolFormatter implements IProtocolFormatter
{
    public InteroperableProtocolFormatter()
    {
        this(true);
    }

    public InteroperableProtocolFormatter(boolean isLittleEndian)
    {
        myIsLittleEndian = isLittleEndian;
    }

    @Override
    public Object encodeOpenConnectionMessage(String responseReceiverId)
            throws Exception
    {
        // An explicit open connection message is not supported.
        return null;
    }
    
    @Override
    public void encodeOpenConnectionMessage(String responseReceiverId, OutputStream outputSream) throws Exception
    {
        // An explicit open connection message is not supported therefore write nothing into the sream.
    }
    
    @Override
    public Object encodeCloseConnectionMessage(String responseReceiverId)
            throws Exception
    {
        // An explicit close connection message is not supported.
        return null;
    }
    
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

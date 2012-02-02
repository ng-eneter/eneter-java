package eneter.messaging.messagingsystems.connectionprotocols;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;


import eneter.messaging.diagnostic.EneterTrace;

public class EneterProtocolFormatter implements IProtocolFormatter<byte[]>
{
    @Override
    public byte[] encodeOpenConnectionMessage(String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream aBuffer = new ByteArrayOutputStream();
            DataOutputStream aWriter = new DataOutputStream(aBuffer);

            encodeHeader(aWriter);

            aWriter.write(OPEN_CONNECTION_REQUEST);

            encodeString(aWriter, responseReceiverId);

            return aBuffer.toByteArray();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public byte[] encodeCloseConnectionMessage(String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream aBuffer = new ByteArrayOutputStream();
            DataOutputStream aWriter = new DataOutputStream(aBuffer);

            encodeHeader(aWriter);

            aWriter.write(CLOSE_CONNECTION_REQUEST);

            encodeString(aWriter, responseReceiverId);

            return aBuffer.toByteArray();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public byte[] encodeMessage(String responseReceiverId, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream aBuffer = new ByteArrayOutputStream();
            DataOutputStream aWriter = new DataOutputStream(aBuffer);

            encodeHeader(aWriter);

            aWriter.write(REQUEST_MESSAGE);

            encodeString(aWriter, responseReceiverId);
            
            encodeMessage(aWriter, message);

            return aBuffer.toByteArray();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public byte[] encodePollRequest(String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream aBuffer = new ByteArrayOutputStream();
            DataOutputStream aWriter = new DataOutputStream(aBuffer);

            encodeHeader(aWriter);

            aWriter.write(POLL_REQUEST);

            encodeString(aWriter, responseReceiverId);

            return aBuffer.toByteArray();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public ProtocolMessage decodeMessage(InputStream readStream)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                DataInputStream aReader = new DataInputStream(readStream);
                
                byte[] h = new byte[6];
                aReader.readFully(h, 0, 6);
                
                // Read the header to recognize if it is the correct protocol.
                if (h[0] != 'E' || h[1] != 'N' || h[2] != 'E' || h[3] != 'T' || h[4] != 'E' || h[5] != 'R')
                {
                    EneterTrace.warning(TracedObject() + "detected unknown protocol format.");
                    return myNonProtocolMessage;
                }

                // Get endian encoding (Big Endian or Little Endian)
                int anEndianEncodingId = readStream.read();
                if (anEndianEncodingId == -1)
                {
                    // End of the stream.
                    return null;
                }
                if (anEndianEncodingId != LITTLE_ENDIAN && anEndianEncodingId != BIG_ENDIAN)
                {
                    EneterTrace.warning(TracedObject() + "detected unknown endian encoding.");
                    return myNonProtocolMessage;
                }


                // Get string encoding (UTF8 or UTF16)
                int aStringEncodingId = readStream.read();
                if (aStringEncodingId == -1)
                {
                    // End of the stream.
                    return null;
                }
                if (aStringEncodingId != UTF8 && aStringEncodingId != UTF16)
                {
                    EneterTrace.warning(TracedObject() + "detected unknown string encoding.");
                    return myNonProtocolMessage;
                }


                // Get the message type.
                int aMessageType = readStream.read();
                if (aMessageType == -1)
                {
                    // End of the stream.
                    return null;
                }

                ProtocolMessage aProtocolMessage;

                if (aMessageType == OPEN_CONNECTION_REQUEST)
                {
                    aProtocolMessage = decodeRequest(EProtocolMessageType.OpenConnectionRequest, aReader, anEndianEncodingId, aStringEncodingId);
                }
                else if (aMessageType == CLOSE_CONNECTION_REQUEST)
                {
                    aProtocolMessage = decodeRequest(EProtocolMessageType.CloseConnectionRequest, aReader, anEndianEncodingId, aStringEncodingId);
                }
                else if (aMessageType == POLL_REQUEST)
                {
                    aProtocolMessage = decodeRequest(EProtocolMessageType.PollRequest, aReader, anEndianEncodingId, aStringEncodingId);
                }
                else if (aMessageType == REQUEST_MESSAGE)
                {
                    aProtocolMessage = decodeMessage(aReader, anEndianEncodingId, aStringEncodingId);
                }
                else
                {
                    EneterTrace.warning(TracedObject() + "detected unknown string encoding.");
                    aProtocolMessage = myNonProtocolMessage;
                }

                return aProtocolMessage;
            }
            catch (IOException err)
            {
                // End of the stream.
                return null;
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "failed to decode the message.", err);
                
                // Invalid message.
                // Note: Just because somebody sends and invalid string the loop reading messages should
                //       not be disturbed/interrupted by an exception.
                //       The reading must continue with the next message.
                return myNonProtocolMessage;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public ProtocolMessage decodeMessage(Object readMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (readMessage instanceof byte[] == false &&
                readMessage instanceof Byte[] == false)
            {
                String anErrorMessage = TracedObject() + "detected that data to be deceoded is not byte[] nor Byte[].";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            ByteArrayInputStream aMemoryStream = new ByteArrayInputStream((byte[])readMessage);
            return decodeMessage(aMemoryStream);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private void encodeHeader(DataOutputStream writer) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Header indicating the Eneter protocol with used Endian encoding.
            // Note: .NET uses Little Endian and UTF8
            byte[] aHeader = {'E', 'N', 'E', 'T', 'E', 'R', BIG_ENDIAN, UTF16};
            writer.write(aHeader);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void encodeMessage(DataOutputStream writer, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (message instanceof String)
            {
                writer.write(STRING);
                encodeString(writer, (String)message);
            }
            else if (message instanceof byte[] || message instanceof Byte[])
            {
                byte[] aBytes = (byte[])message;

                writer.writeByte(BYTES);
                writer.writeInt(aBytes.length);
                writer.write(aBytes);
            }
            else
            {
                String anErrorMessage = "The message is not serialized to string or byte[].";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private ProtocolMessage decodeRequest(EProtocolMessageType messageType, DataInputStream reader, int anEndianEncodingId, int aStringEncodingId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aResponseReceiverId = getResponseReceiverId(reader, anEndianEncodingId, aStringEncodingId);

            ProtocolMessage aProtocolMessage = new ProtocolMessage(messageType, aResponseReceiverId, null);
            return aProtocolMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private ProtocolMessage decodeMessage(DataInputStream reader, int anEndianEncodingId, int aStringEncodingId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aResponseReceiverId = getResponseReceiverId(reader, anEndianEncodingId, aStringEncodingId);
            Object aMessage = getMessage(reader, anEndianEncodingId, aStringEncodingId);

            ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.MessageReceived, aResponseReceiverId, aMessage);
            return aProtocolMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private String getResponseReceiverId(DataInputStream reader, int anEndianEncodingId, int aStringEncodingId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            int aSize = readInt(reader, anEndianEncodingId);

            byte[] aBytes = new byte[aSize];
            reader.readFully(aBytes);

            String aResponseReceiverId = decodeString(aBytes, anEndianEncodingId, aStringEncodingId);

            return aResponseReceiverId;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private Object getMessage(DataInputStream reader, int anEndianEncodingId, int aStringEncodingId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            int aSerializationType = reader.read();
            
            int aSize = readInt(reader, anEndianEncodingId);

            byte[] aBytes = new byte[aSize];
            reader.readFully(aBytes);

            if (aSerializationType == BYTES)
            {
                return aBytes;
            }

            if (aSerializationType == STRING)
            {
                String aResult = decodeString(aBytes, anEndianEncodingId, aStringEncodingId);
                return aResult;
            }

            String anErrorMessage = "Received message is not serialized into byte[] or string.";
            EneterTrace.error(anErrorMessage);
            throw new IllegalStateException(anErrorMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private int readInt(DataInputStream reader, int anEndianEncodingId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            int anInt = reader.readInt();
            
            // Convert to big endian, if incoming data is in little endian.
            if (anEndianEncodingId == LITTLE_ENDIAN)
            {
                anInt = ((anInt & 0x000000ff) << 24) + ((anInt & 0x0000ff00) << 8) +
                        ((anInt & 0x00ff0000) >>> 8) + ((anInt & 0xff000000) >>> 24);
            }
            
            return anInt;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void encodeString(DataOutputStream writer, String s) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Charset aCharset = Charset.forName("UTF-16BE");
            ByteBuffer aByteBuffer = aCharset.encode(s);
            
            // Actual size of data.
            writer.writeInt(aByteBuffer.limit());
            
            // Write data.
            writer.write(aByteBuffer.array(), 0, aByteBuffer.limit());
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private String decodeString(byte[] encodedString, int anEndianEncodingId, int aStringEncodingId)
            throws UnsupportedEncodingException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aCharset;
            
            if (aStringEncodingId == UTF16 && anEndianEncodingId == BIG_ENDIAN)
            {
                aCharset = "UTF-16BE";
            }
            else if (aStringEncodingId == UTF16 && anEndianEncodingId == LITTLE_ENDIAN)
            {
                aCharset = "UTF-16LE";
            }
            else if (aStringEncodingId == UTF8)
            {
                aCharset = "UTF-8";
            }
            else
            {
                String anErrorMessage = TracedObject() + "detected unknown string encoding.";
                EneterTrace.warning(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
               
            String aResult = new String(encodedString, aCharset);
            return aResult;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    // Type of encoding for numbers.
    private final byte LITTLE_ENDIAN = 10;
    private final byte BIG_ENDIAN = 20;

    // Type of encoding for strings.
    private final byte UTF8 = 10;
    private final byte UTF16 = 20;

    // Type of serialization.
    // E.g. If the message is serialized into XML, the it will indicate the type is string.
    //      If the message is serialized in a binary format, then it will indicate the type is byte[].
    private final byte BYTES = 10;
    private final byte STRING = 20;

    // Type of low level message - used for the low level communication between channels.
    private final byte OPEN_CONNECTION_REQUEST = 10;
    private final byte CLOSE_CONNECTION_REQUEST = 20;
    private final byte POLL_REQUEST = 30;
    private final byte REQUEST_MESSAGE = 40;
    
    private static final ProtocolMessage myNonProtocolMessage = new ProtocolMessage(EProtocolMessageType.Unknown, "", null);
    
    private String TracedObject()
    {
        return "EneterProtocolFormatter ";
    }
}

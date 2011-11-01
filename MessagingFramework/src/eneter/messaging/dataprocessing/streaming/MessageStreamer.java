package eneter.messaging.dataprocessing.streaming;

import java.io.*;

import eneter.messaging.diagnostic.EneterTrace;

public final class MessageStreamer
{
    public static Object readMessage(InputStream readingStream) throws IOException
    {
        ObjectInputStream aStreamReader = new ObjectInputStream(readingStream);
        Object aMessage = readMessage(aStreamReader);
        
        return aMessage;
    }
    
    private static Object readMessage(ObjectInputStream reader) throws IOException
    {
        try
        {
            byte aType = reader.readByte();
            
            // Read the count.x
            int aNumberOfParts = 0;
            if (aType == OBJECTS || aType == BYTES || aType == CHARS)
            {
                aNumberOfParts = reader.readInt();
                if (aNumberOfParts <= 0)
                {
                    // If the stream was closed
                    return null;
                }
                
                // Read the content.
                if (aType == OBJECTS)
                {
                    Object[] anObjects = new Object[aNumberOfParts];

                    for (int i = 0; i < aNumberOfParts; ++i)
                    {
                        anObjects[i] = readMessage(reader);

                        // If the stream was closed
                        if (anObjects[i] == null)
                        {
                            return null;
                        }
                    }

                    return anObjects;
                }
                else if (aType == BYTES)
                {
                    byte[] aBytes = new byte[aNumberOfParts];
                    reader.read(aBytes);
                    return aBytes;
                }
                else if (aType == BYTE)
                {
                    byte aByte = reader.readByte();
                    return aByte;
                }
                else if (aType == STRING)
                {
                    String aString = reader.readUTF();
                    return aString;
                }
            }
        }
        catch (EOFException err)
        {
            return null;
        }
        
        String anErrorMessage = "Reading the message from the stream failed. If the serialized message consists of more serialized parts these parts must be object[] or byte[] or string or char[].";
        EneterTrace.error(anErrorMessage);
        throw new IllegalStateException(anErrorMessage);
    }
    
    public static void writeMessage(OutputStream writingStream, Object message) throws IOException
    {
        ObjectOutputStream aStreamWriter = new ObjectOutputStream(writingStream);
        writeMessage(aStreamWriter, message);
    }
    
    private static void writeMessage(ObjectOutputStream writer, Object message) throws IOException
    {
        if (message instanceof Object[])
        {
            int aNumberOfParts = ((Object[])message).length;
            
            writer.writeByte(BYTES);
            writer.writeInt(aNumberOfParts);
            
            // Go recursively down through all parts of the message.
            for (Object o : (Object[])message)
            {
                writeMessage(writer, o);
            }
        }
        else if (message instanceof byte[])
        {
            int aNumberOfParts = ((byte[])message).length;
            
            writer.writeByte(BYTES);
            writer.writeInt(aNumberOfParts);
            writer.write((byte[])message);
        }
        else if (message instanceof String)
        {
            writer.writeByte(BYTES);
            writer.writeUTF((String)message);
        }
        else if (message instanceof Byte)
        {
            writer.writeByte(BYTE);
            writer.writeByte((Byte)message);
        }
        else
        {
            String anErrorMessage = "Writing of message to the stream failed because the message is not serialized to String or Byte[]";
            EneterTrace.error(anErrorMessage);
            throw new IllegalStateException(anErrorMessage);
        }
    }
    
    public static void writeOpenConnectionMessage(OutputStream writingStream, String responseReceiverId)
            throws IOException
    {
        Object[] aMessage = getOpenConnectionMessage(responseReceiverId);
        writeMessage(writingStream, aMessage);
    }
    
    public static Object[] getOpenConnectionMessage(String responseReceiverId)
    {
        Object[] aMessage = { (byte)OPENCONNECION, responseReceiverId };
        return aMessage;
    }
    
    public static boolean isOpenConnectionMessage(Object message)
    {
        if (message instanceof Object[] &&
            ((Object[])message).length == 2 && ((Object[])message)[0] instanceof Byte &&
            (Byte)((Object[])message)[0] == OPENCONNECION)
        {
            return true;
        }

        return false;
    }
    
    
    public static void writeCloseConnectionMessage(OutputStream writingStream, String responseReceiverId)
        throws IOException
    {
        Object[] aMessage = getCloseConnectionMessage(responseReceiverId);
        writeMessage(writingStream, aMessage);
    }
    
    public static Object[] getCloseConnectionMessage(String responseReceiverId)
    {
        Object[] aMessage = { (byte)CLOSECONNECTION, responseReceiverId };
        return aMessage;
    }
    
    public static boolean isCloseConnectionMessage(Object message)
    {
        if (message instanceof Object[] &&
            ((Object[])message).length == 2 && ((Object[])message)[0] instanceof Byte &&
            (Byte)((Object[])message)[0] == CLOSECONNECTION)
        {
            return true;
        }

        return false;
    }
    
    
    public static void writeRequestMessage(OutputStream writingStream, String responseReceiverId, Object message)
            throws IOException
    {
        Object[] aMessage = { (byte)REQUEST, responseReceiverId, message };
        writeMessage(writingStream, aMessage);
    }
    
    public static boolean isRequestMessage(Object message)
    {
        if (message instanceof Object[] &&
            ((Object[])message).length == 3 && ((Object[])message)[0] instanceof Byte &&
            (Byte)((Object[])message)[0] == REQUEST)
        {
            return true;
        }

        return false;
    }
    
    
    public static void writePollResponseMessage(OutputStream writingStream, String responseReceiverId)
            throws IOException
    {
        writeMessage(writingStream, responseReceiverId);
    }
    
    public static boolean isPollResponseMessage(Object message)
    {
        return message instanceof String;
    }
    
    
    // Type of serialization.
    private static final byte OBJECTS = 01;
    private static final byte BYTES = 02;
    private static final byte BYTE = 03;
    private static final byte STRING = 04;
    private static final byte CHARS = 05;

    // Type of low level message - used for the low level communication between channels.
    private static final byte OPENCONNECION = 0;
    private static final byte CLOSECONNECTION = 1;
    private static final byte REQUEST = 2;
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.streaming;

import java.io.*;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Provides functionality to read and write specific messages internaly used by duplex channels.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public final class MessageStreamer
{
    /**
     * Reads the message from the stream.
     * @param readingStream stream to be read
     * @return message
     * @throws IOException if the reading of the stream fails.
     */
    public static Object readMessage(InputStream readingStream) throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            DataInputStream aStreamReader = new DataInputStream(readingStream);
            Object aMessage = readMessage(aStreamReader);
            
            return aMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private static Object readMessage(DataInputStream reader) throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                byte aType = reader.readByte();
                
                // Read the count.x
                int aNumberOfParts = 0;
                if (aType == OBJECTS || aType == BYTES)
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
                }
                
                if (aType == STRING)
                {
                    String aString = reader.readUTF();
                    return aString;
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
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    /**
     * Writes the message to the specified stream.
     * @param writingStream stream where the message will be put
     * @param message message
     * @throws IOException if the writing to the stream fails.
     */
    public static void writeMessage(OutputStream writingStream, Object message) throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            DataOutputStream aStreamWriter = new DataOutputStream(writingStream);
            writeMessage(aStreamWriter, message);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private static void writeMessage(DataOutputStream writer, Object message) throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (message instanceof Object[])
            {
                int aNumberOfParts = ((Object[])message).length;
                
                writer.writeByte(OBJECTS);
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
                writer.writeByte(STRING);
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
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Writes the message used by duplex output channels to open connection with the duplex input channel.
     * @param writingStream Stream where the message is written.
     * @param responseReceiverId Id of receiver of response messages.
     * @throws IOException if writing to the stream fails
     */
    public static void writeOpenConnectionMessage(OutputStream writingStream, String responseReceiverId)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object[] aMessage = getOpenConnectionMessage(responseReceiverId);
            writeMessage(writingStream, aMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns the low level communication message used by duplex output channels to open connection with duplex input channel.
     * @param responseReceiverId id of receiver of response messages.
     * @return open connection message
     */
    public static Object[] getOpenConnectionMessage(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object[] aMessage = { (byte)OPENCONNECION, responseReceiverId };
            return aMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns true if the given message is the open connection message used by the duplex output channel to open connection.
     * @param message message
     * @return true if the given message is open connection message
     */
    public static boolean isOpenConnectionMessage(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (message instanceof Object[] &&
                ((Object[])message).length == 2 && ((Object[])message)[0] instanceof Byte &&
                (Byte)((Object[])message)[0] == OPENCONNECION)
            {
                return true;
            }
    
            return false;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Writes the message used by the duplex output channel to close the connection with the duplex input channel.
     * @param writingStream Stream where the message is written.
     * @param responseReceiverId Id of receiver of response messages.
     * @throws IOException if writing to the stream fails.
     */
    public static void writeCloseConnectionMessage(OutputStream writingStream, String responseReceiverId)
        throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object[] aMessage = getCloseConnectionMessage(responseReceiverId);
            writeMessage(writingStream, aMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns the low level communication message used by duplex output channel to close the connection with duplex input channel.
     * @param responseReceiverId
     * @return
     */
    public static Object[] getCloseConnectionMessage(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object[] aMessage = { (byte)CLOSECONNECTION, responseReceiverId };
            return aMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns true if the given message is the message used by the duplex output channel to close the connection.
     * @param message
     * @return true if the given message is the close connection message
     */
    public static boolean isCloseConnectionMessage(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (message instanceof Object[] &&
                ((Object[])message).length == 2 && ((Object[])message)[0] instanceof Byte &&
                (Byte)((Object[])message)[0] == CLOSECONNECTION)
            {
                return true;
            }
    
            return false;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Writes the request message used by the duplex output channel to send to the duplex input channel.
     * @param writingStream Stream where the message is written.
     * @param responseReceiverId Id of receiver of response messages.
     * @param message Request message.
     * @throws IOException if writing to the stream fails.
     */
    public static void writeRequestMessage(OutputStream writingStream, String responseReceiverId, Object message)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object[] aMessage = getRequestMessage(responseReceiverId, message);
            writeMessage(writingStream, aMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns the request message used by the duplex output channel to send to the duplex input channel.
     * @param responseReceiverId Id of receiver of response messages.
     * @param message Request message.
     * @return request message encoded to be understood by the duplex input channel.
     */
    public static Object[] getRequestMessage(String responseReceiverId, Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object[] aMessage = { (byte)REQUEST, responseReceiverId, message };
            return aMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns true if the given message is the request message used by the duplex output channel to send
     * a message to the duplex input channel.
     * @param message
     * @return
     */
    public static boolean isRequestMessage(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (message instanceof Object[] &&
                ((Object[])message).length == 3 && ((Object[])message)[0] instanceof Byte &&
                (Byte)((Object[])message)[0] == REQUEST)
            {
                return true;
            }

            return false;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Writes the message used by the duplex output channel to poll response messages from the
     * duplex input channel. The message is used in case of Http messaging.
     * @param writingStream Stream where the message is written.
     * @param responseReceiverId Id of receiver of response messages.
     * @throws IOException if writing to the stream fails.
     */
    public static void writePollResponseMessage(OutputStream writingStream, String responseReceiverId)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            writeMessage(writingStream, responseReceiverId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns true if the given message is the essage used by the duplex output channel to poll
     * messages from the duplex input channel. (in case of Http)
     * @param message
     * @return
     */
    public static boolean isPollResponseMessage(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return message instanceof String;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
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

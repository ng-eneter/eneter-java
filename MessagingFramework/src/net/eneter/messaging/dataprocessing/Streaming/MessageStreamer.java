package net.eneter.messaging.dataprocessing.Streaming;

import java.io.*;
import java.util.*;

public class MessageStreamer
{
    // Type of serialization.
    private final static byte OBJECTS = 01;
    private final static byte BYTES = 02;
    private final static byte BYTE = 03;
    private final static byte STRING = 04;
    private final static byte CHARS = 05;
	
    /// <summary>
    /// Writes the message to the specified stream.
    /// </summary>
    /// <param name="writingStream"></param>
    /// <param name="message"></param>
    public static void WriteMessage(OutputStream writingStream, Object message)
    {
    	DataOutputStream aStreamWriter = new DataOutputStream(writingStream);
        WriteMessage(aStreamWriter, message);
    }

    private static void WriteMessage(DataOutputStream writer, Object message)
    {
        if (message instanceof Collection<?>)
        {
            // Store number of parts
            int aNumberOfParts = ((Collection<?>)message).size();

            // Store type of parts
            if (message instanceof Collection<Object>)
            {
                writer.write(OBJECTS);
                writer.write(aNumberOfParts);

                // Go recursively down through all parts of the message.
                for (Object o : (Object[])message)
                {
                    WriteMessage(writer, o);
                }
            }
            else if (message instanceof byte[])
            {
                writer.write(BYTES);
                writer.write(aNumberOfParts);
                writer.write((byte[])message);
            }
            else if (message instanceof char[])
            {
                writer.write(CHARS);
                writer.write(aNumberOfParts);
                for (char c : (char[])message)
                {
                	writer.writeChar(c);
                }
            }
            else
            {
                string anErrorMessage = "Writing the message to the stream failed. If the serialized message consists of more serialized parts these parts must be object[] or byte[] or string or char[].";
                EneterTrace.Error(anErrorMessage);
                throw new InvalidOperationException(anErrorMessage);
            }
        }
        else if (message is string)
        {
            writer.Write(STRING);
            writer.Write((string)message);
        }
        else if (message is byte)
        {
            writer.Write(BYTE);
            writer.Write((byte)message);
        }
        else
        {
            string anErrorMessage = "It is not possible to get number of message parts because it is not type of ICollection.";
            EneterTrace.Error(anErrorMessage);
            throw new InvalidOperationException(anErrorMessage);
        }
    }

}

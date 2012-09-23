/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


package eneter.messaging.dataprocessing.streaming.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eneter.messaging.diagnostic.EneterTrace;

public final class StreamUtil
{
    
    public static byte[] readBytes(InputStream inputStream, int length)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream anOutputMemStream = new ByteArrayOutputStream();
            try
            {
                readBytes(inputStream, length, anOutputMemStream);

                return anOutputMemStream.toByteArray();
            }
            finally
            {
                anOutputMemStream.close();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static void readBytes(InputStream inputStream, int length, OutputStream outputStream)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            byte[] aBuffer = new byte[length];
            int aRemainingSize = length;
            while (aRemainingSize > 0)
            {
                int aSize = inputStream.read(aBuffer, 0, aBuffer.length);

                if (aSize == 0)
                {
                    // Unexpected end of stream.
                    throw new IOException();
                }

                outputStream.write(aBuffer, 0, aSize);

                aRemainingSize -= aSize;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
}

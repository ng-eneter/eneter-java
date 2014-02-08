/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.streaming.internal;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Internal class for encoding/decoding serialized data.
 * It stores informarmation about used encoding (UTF8 or UTF16) and Little Endian and Big Endian number encoding.
 * It helps to ensure compatibility between various platforms.
 *
 */
public class EncoderDecoder
{
    public void encode(OutputStream writer, Object serializedData)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Encrypt serialized data.
            if (serializedData instanceof String)
            {
                String aSerializedStr = (String)serializedData;
                
                // Note: UTF-16 Big Endian is native Java encoding.
                Charset aCharset = Charset.forName("UTF-16BE");
                ByteBuffer aByteBuffer = aCharset.encode(aSerializedStr);
                byte[] aDataToEncode = aByteBuffer.array();
                
                // Write info that encoded data is UTF16 Big Endian.
                writer.write(STRING_UTF16_BE_ID);
                
                // Encode data.
                writer.write(aDataToEncode);
            }
            else if (serializedData instanceof byte[] ||
                     serializedData instanceof Byte[])
            {
                // Write info, that encoded data is array of bytes.
                writer.write(BYTES_ID);
                
                // Encode data.
                writer.write((byte[])serializedData);
            }
            else
            {
                String anErrorMessage = "Encoding of data failed because the underlying serializer does not serialize to String or byte[].";
                EneterTrace.error(anErrorMessage);
                throw new IllegalAccessError(anErrorMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public Object decode(InputStream reader)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object aDecodedData = null;
            
            // Read type of data.
            int aDataType = reader.read();
            if (aDataType == -1)
            {
                String anErrorMessage = "Decoding of serialized data failed because of unexpected end of stream.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            if (aDataType != STRING_UTF8_ID &&
                aDataType != STRING_UTF16_LE_ID &&
                aDataType != STRING_UTF16_BE_ID &&
                aDataType != BYTES_ID)
            {
                String anErrorMessage = "Decoding of serialized data failed because of incorrect data fromat.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            // Read bytes from the stream until the end.
            ByteArrayOutputStream aDecodedBytes = new ByteArrayOutputStream();
            byte[] aBuffer = new byte[32000];
            int aSize;
            while ((aSize = reader.read(aBuffer)) > 0)
            {
                aDecodedBytes.write(aBuffer, 0, aSize);
            }
            
            if (aDataType == STRING_UTF8_ID)
            {
                aDecodedData = new String(aDecodedBytes.toByteArray(), "UTF-8");
            }
            else if (aDataType == STRING_UTF16_LE_ID)
            {
                aDecodedData = new String(aDecodedBytes.toByteArray(), "UTF-16LE");
            }
            else if (aDataType == STRING_UTF16_BE_ID)
            {
                aDecodedData = new String(aDecodedBytes.toByteArray(), "UTF-16BE");
            }
            else if (aDataType == BYTES_ID)
            {
                aDecodedData = aDecodedBytes.toByteArray();
            }
            else
            {
                String anErrorMessage = "Decoding of serialized data failed because of incorrect data fromat.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            return aDecodedData;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    private final byte STRING_UTF8_ID = 10;
    private final byte STRING_UTF16_LE_ID = 20;
    private final byte STRING_UTF16_BE_ID = 30;
    private final byte BYTES_ID = 40;
}

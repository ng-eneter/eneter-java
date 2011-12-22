package eneter.messaging.dataprocessing.serializing;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.zip.*;

import eneter.messaging.diagnostic.EneterTrace;

public class GZipSerializer implements ISerializer
{
    public GZipSerializer(ISerializer underlyingSerializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingSerializer = underlyingSerializer;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Use underlying serializer to serialize data.
            Object aSerializedData = myUnderlyingSerializer.serialize(dataToSerialize, clazz);
            
            ByteArrayOutputStream aCompressedData = new ByteArrayOutputStream();
            
            GZIPOutputStream aGzipOutputStream = new GZIPOutputStream(aCompressedData);
            
            // Compress serialized data.
            if (aSerializedData instanceof String)
            {
                String aSerializedStr = (String)aSerializedData;
                
                // Note: UTF-16 Big Endian is native Java encoding.
                Charset aCharset = Charset.forName("UTF-16BE");
                ByteBuffer aByteBuffer = aCharset.encode(aSerializedStr);
                byte[] aDataToCompress = aByteBuffer.array();
                
                // Write info that compressed data is UTF16 Big Endian.
                aGzipOutputStream.write(STRING_UTF16_BE_ID);
                
                // Compress data.
                aGzipOutputStream.write(aDataToCompress);
            }
            else if (aSerializedData instanceof byte[] ||
                     aSerializedData instanceof Byte[])
            {
                // Write info, that compressed data is array of bytes.
                aGzipOutputStream.write(BYTES_ID);
                
                // Compress data.
                aGzipOutputStream.write((byte[])aSerializedData);
            }
            else
            {
                String anErrorMessage = TracedObject() + "failed to compress data because the underlying serializer serializes data into incorrect type.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalAccessError(anErrorMessage);
            }
            
            
            aGzipOutputStream.finish();
            return aCompressedData.toByteArray();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <T> T deserialize(Object serializedData, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            byte[] aCompressedData = (byte[])serializedData;

            Object aDecompressedData = null;
            
            // Put compressed data to the stream.
            ByteArrayInputStream aCompressedStream = new ByteArrayInputStream(aCompressedData);
            
            // Create the GZipStream to decompress data.
            GZIPInputStream aGzipStream = new GZIPInputStream(aCompressedStream);
            
            // Read type of data.
            int aDataType = aGzipStream.read();
            if (aDataType == -1)
            {
                String anErrorMessage = TracedObject() + "failed to deserialize the object because of unexpected end of stream.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            if (aDataType != STRING_UTF8_ID &&
                aDataType != STRING_UTF16_LE_ID &&
                aDataType != STRING_UTF16_BE_ID &&
                aDataType != BYTES_ID)
            {
                String anErrorMessage = TracedObject() + "failed to deserialize the object because of incorrect data fromat.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            // Read bytes from the stream until the end.
            ByteArrayOutputStream aDecompressedBytes = new ByteArrayOutputStream();
            byte[] aBuffer = new byte[32000];
            int aSize;
            while ((aSize = aGzipStream.read(aBuffer)) > 0)
            {
                aDecompressedBytes.write(aBuffer, 0, aSize);
            }
            
            if (aDataType == STRING_UTF8_ID)
            {
                Charset aCharset = Charset.forName("UTF-8");
                aDecompressedData = new String(aDecompressedBytes.toByteArray(), aCharset);
            }
            else if (aDataType == STRING_UTF16_LE_ID)
            {
                Charset aCharset = Charset.forName("UTF-16LE");
                aDecompressedData = new String(aDecompressedBytes.toByteArray(), aCharset);
            }
            else if (aDataType == STRING_UTF16_BE_ID)
            {
                Charset aCharset = Charset.forName("UTF-16BE");
                aDecompressedData = new String(aDecompressedBytes.toByteArray(), aCharset);
            }
            else if (aDataType == BYTES_ID)
            {
                aDecompressedData = aDecompressedBytes.toByteArray();
            }
            else
            {
                String anErrorMessage = TracedObject() + "failed to deserialize the object because of incorrect data fromat.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            // Use underlying serializer to deserialize data.
            return myUnderlyingSerializer.deserialize(aDecompressedData, clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    
    
    private ISerializer myUnderlyingSerializer;
    
    private final byte STRING_UTF8_ID = 10;
    private final byte STRING_UTF16_LE_ID = 20;
    private final byte STRING_UTF16_BE_ID = 30;
    private final byte BYTES_ID = 40;
    
    private String TracedObject()
    {
        return "GZipSerializer ";
    }
}

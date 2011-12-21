package eneter.messaging.dataprocessing.serializing;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.*;

import eneter.messaging.diagnostic.EneterTrace;

public class GZipSerializer implements ISerializer
{

    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
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
                String aDeserializedData = new String(aDecompressedBytes.toByteArray(), aCharset);
                return (T)aDeserializedData;
            }
            
            if (aDataType == STRING_UTF16_LE_ID)
            {
                Charset aCharset = Charset.forName("UTF-16LE");
                String aDeserializedData = new String(aDecompressedBytes.toByteArray(), aCharset);
                return (T)aDeserializedData;
            }
            
            if (aDataType == STRING_UTF16_BE_ID)
            {
                Charset aCharset = Charset.forName("UTF-16BE");
                String aDeserializedData = new String(aDecompressedBytes.toByteArray(), aCharset);
                return (T)aDeserializedData;
            }
            
            if (aDataType == BYTES_ID)
            {
                return (T)aDecompressedBytes.toByteArray();
            }

            String anErrorMessage = TracedObject() + "failed to deserialize the object because of incorrect data fromat.";
            EneterTrace.error(anErrorMessage);
            throw new IllegalStateException(anErrorMessage);
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

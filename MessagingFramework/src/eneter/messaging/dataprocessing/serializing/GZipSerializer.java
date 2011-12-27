package eneter.messaging.dataprocessing.serializing;

import java.io.*;
import java.util.zip.*;

import eneter.messaging.diagnostic.EneterTrace;

public class GZipSerializer implements ISerializer
{
    public GZipSerializer(ISerializer underlyingSerializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myEncoderDecoder = new EncoderDecoder(underlyingSerializer);
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
            ByteArrayOutputStream aCompressedData = new ByteArrayOutputStream();
            GZIPOutputStream aGzipOutputStream = new GZIPOutputStream(aCompressedData);
            
            myEncoderDecoder.serialize(aGzipOutputStream, dataToSerialize, clazz);
            
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

            // Put compressed data to the stream.
            ByteArrayInputStream aCompressedStream = new ByteArrayInputStream(aCompressedData);
            
            // Create the GZipStream to decompress data.
            GZIPInputStream aGzipStream = new GZIPInputStream(aCompressedStream);

            return myEncoderDecoder.deserialize(aGzipStream, clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private EncoderDecoder myEncoderDecoder;
}

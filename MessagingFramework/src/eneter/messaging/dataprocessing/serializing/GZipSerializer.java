/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

import java.io.*;
import java.util.zip.*;

import eneter.messaging.dataprocessing.streaming.internal.EncoderDecoder;
import eneter.messaging.diagnostic.EneterTrace;

/**
 * Serializer compressing and decompressing data.
 * The serializer internally uses GZipStream to compress and decompress data.
 * <pre>
 * Example shows how to serialize data.
 * <br/>
 * {@code
 * // Creat the serializer.
 * GZipSerializer aSerializer = new GZipSerializer();
 *
 * // Create some data to be serialized.
 * MyData aData = new MyData();
 * ...
 *
 * // Serialize data. Serialized data will be compressed.
 * object aSerializedData = aSerializer.serialize(aData, MyData.class);
 * ...
 *
 * // Deserialize data
 * MyData aDeserialized = aSerializer.deserialize(aSerializedData, MyData.class);
 * }
 * </pre>
 *
 */
public class GZipSerializer implements ISerializer
{
    /**
     * Constructs the serializer with XmlStringSerializer as the underlying serializer.
     * The serializer uses the underlying serializer to serialize data before the compression.
     * It also uses the underlying serializer to deserialize decompressed data.
     */
    public GZipSerializer()
    {
        this(new XmlStringSerializer());
    }

    /**
     * Constructs the serializer with the given underlying serializer.
     * The serializer uses the underlying serializer to serialize data before the compression.
     * It also uses the underlying serializer to deserialize decompressed data.
     * 
     * @param underlyingSerializer
     */
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

    /**
     * Serializes the given data with using the compression.
     */
    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream aCompressedData = new ByteArrayOutputStream();
            GZIPOutputStream aGzipOutputStream = new GZIPOutputStream(aCompressedData);
            
            // Use underlying serializer to serialize data.
            Object aSerializedData = myUnderlyingSerializer.serialize(dataToSerialize, clazz);
            myEncoderDecoder.encode(aGzipOutputStream, aSerializedData);
            
            aGzipOutputStream.finish();
            return aCompressedData.toByteArray();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Deserializes compressed data into the specified type.
     */
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

            Object aDecodedData = myEncoderDecoder.decode(aGzipStream);
            T aDeserializedData = myUnderlyingSerializer.deserialize(aDecodedData, clazz);
            return aDeserializedData;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private ISerializer myUnderlyingSerializer;
    private EncoderDecoder myEncoderDecoder = new EncoderDecoder();
}

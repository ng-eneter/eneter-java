/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements the wrapper/unwrapper of data.
 *
 */
public class DataWrapper
{
    /**
     * Adds the data to already serialized data.
     * It creates the WrappedData from the given data and serializes it with the provided serializer.<br/>
     * @param addedData Added data. It must a basic .Net type. Otherwise the serialization will fail.
     * @param originalData Already serialized data - it is type of string or byte[].
     * @param serializer serializer
     * @return
     * @throws Exception
     */
    public static Object wrap(Object addedData, Object originalData, ISerializer serializer) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            WrappedData aWrappedData = new WrappedData(addedData, originalData);
            return serializer.serialize(aWrappedData, WrappedData.class);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    /**
     * Takes the serialized WrappedData and deserializes it with the given serializer.
     * @param wrappedData data serialized by 'Wrap' method
     * @param serializer serializer
     * @return deserialized WrappedData
     * @throws Exception
     */
    public static WrappedData unwrap(Object wrappedData, ISerializer serializer) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return serializer.deserialize(wrappedData, WrappedData.class);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
}

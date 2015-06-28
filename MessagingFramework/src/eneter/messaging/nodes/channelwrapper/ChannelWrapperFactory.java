/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;


/**
 * Factory for creating channel wrapper and unwrapper.
 *
 */
public class ChannelWrapperFactory implements IChannelWrapperFactory
{
    /**
     * Constructs the factory.
     */
    public ChannelWrapperFactory()
    {
        this(new XmlStringSerializer());
    }

    /**
     * Constructs the factory.
     * @param serializer serializer used for wrapping channels with data messages.
     */
    public ChannelWrapperFactory(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
            mySerializerProvider = null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public IDuplexChannelWrapper createDuplexChannelWrapper()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexChannelWrapper(mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexChannelUnwrapper createDuplexChannelUnwrapper(IMessagingSystemFactory outputMessagingSystem)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexChannelUnwrapper(outputMessagingSystem, mySerializer, mySerializerProvider);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Sets serializer which shall be used to serialize/deserialize DataWrapper.
     * @param serializer serializer
     * @return this ChannelWrapperFactory
     */
    public ChannelWrapperFactory setSerializer(ISerializer serializer)
    {
        mySerializer = serializer;
        return this;
    }
    
    /**
     * Gets serializer which is used to serialize/deserialize DataWrapper.
     * @return serializer
     */
    public ISerializer getSerializer()
    {
        return mySerializer;
    }
    
    /**
     * Gets callback for retrieving serializer based on response receiver id.
     * This callback is used by DuplexChannelUnwrapper when it needs to serialize/deserialize the communication with DuplexChannelWrapper.
     * Providing this callback allows to use a different serializer for each connected client.
     * This can be used e.g. if the communication with each client needs to be encrypted using a different password.<br/>
     * <br/>
     * The default value is null and it means SerializerProvider callback is not used and one serializer which specified in the Serializer property is used for all serialization/deserialization.<br/>
     * If SerializerProvider is not null then the setting in the Serializer property is ignored.
     * @return GetSerializerCallback
     */
    public GetSerializerCallback getSerializerProvider()
    {
        return mySerializerProvider;
    }
    
    /**
     * Sets callback for retrieving serializer based on response receiver id.
     * This callback is used by DuplexChannelUnwrapper when it needs to serialize/deserialize the communication with DuplexChannelWrapper.
     * Providing this callback allows to use a different serializer for each connected client.
     * This can be used e.g. if the communication with each client needs to be encrypted using a different password.<br/>
     * <br/>
     * The default value is null and it means SerializerProvider callback is not used and one serializer which specified in the Serializer property is used for all serialization/deserialization.<br/>
     * If SerializerProvider is not null then the setting in the Serializer property is ignored.
     * @param serializerProvider
     * @return
     */
    public ChannelWrapperFactory setSerializerProvider(GetSerializerCallback serializerProvider)
    {
        mySerializerProvider = serializerProvider;
        return this;
    }
    
    private ISerializer mySerializer;
    private GetSerializerCallback mySerializerProvider;
}

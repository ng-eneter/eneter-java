/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

/**
 * Callback used by communication components to get serializer for a specific client connection.
 * If the callback is specified then the service communication component calls it whenever it needs to serialize/deserialize
 * the communicatation with the client. The purpose of this callback is to allow to use for each client a different serializer.
 * E.g. if the serialized message shall be encrypted by a client specific password or a key.
 */
public interface GetSerializerCallback
{
    /**
     * Returns the serializer which shall be used for the specified response receiver id.
     * @param responseReceiverId response receiver id for which the implementation shall return the serializer.
     * @return
     */
    ISerializer invoke(String responseReceiverId);
}

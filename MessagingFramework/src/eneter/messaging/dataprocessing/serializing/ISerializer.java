/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

/**
 * Declares the serializer.
 *
 */
public interface ISerializer
{
    /**
     * Serializes data.
     * 
     * @param dataToSerialize Data to be serialized.
     * @param clazz represents the serialized type.
     * @return Object representing the serialized data.
     *         Based on the serializer implementation it can be byte[] or String.
     * @throws Exception If the serialization fails.
     */
    <T> Object serialize(T dataToSerialize, Class<T> clazz) throws Exception;
    
    /**
     * Deserializes data.
     * 
     * @param serializedData Data to be deserialized.
     * @return Deserialized object.
     * @throws Exception If the deserialization fails.
     */
    <T> T deserialize(Object serializedData, Class<T> clazz) throws Exception;
}

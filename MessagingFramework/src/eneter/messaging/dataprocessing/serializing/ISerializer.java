/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

/**
 * The interface declares the API for serialization and deserialization.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public interface ISerializer
{
    /**
     * Serializes data to Object.
     * @param dataToSerialize Data to be serialized.
     * @param clazz represents the serialized type.
     * @return Object representing the serialized data. Typically it can be byte[] or string.
     * @throws Exception If the serialization fails.
     */
    <T> Object serialize(T dataToSerialize, Class<T> clazz) throws Exception;
    
    /**
     * Deserializes data into the specified type.
     * @param serializedData Data to be deserialized.
     * @return Deserialized object.
     * @throws Exception If the deserialization fails.
     */
    <T> T deserialize(Object serializedData, Class<T> clazz) throws Exception;
}

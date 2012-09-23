/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

import java.io.*;

import eneter.messaging.diagnostic.EneterTrace;


/**
 * Implements the serialization/deserialization to/from sequence of bytes.
 * The serializer internaly uses ObjectOutputStream.
 * The class for the serialization must be derived from Serializable.<br/>
 * <br/>
 * 
 * This binary serializer is not compatible with the binary serializer used
 * in Eneter Messaging Framework for .NET.
 * <pre>
 * Serialization with JavaBinarySerializer.
 * <br/>
 * {@code
 * // Some class to be serialized.
 * public class MyClass implements Serializable
 * {
 *      private static final long serialVersionUID = -8325844480504249827L;
 *      
 *      public String myData;
 * }
 *
 * // Create the serializer.
 * JavaBinarySerializer aSerializer = new JavaBinarySerializer();
 *
 * // Create some data to be serialized.
 * MyClass aData = new MyClass();
 * ...
 *
 * // Serialize data.
 * object aSerializedData = aSerializer.serialize(aData, MyClass.class);
 *
 * // Deserialize data.
 * MyClass aDeserializedData = aSerializer.deserialize(aSerializedData, MyClass.class);
 * }
 * </pre>
 *
 */
public class JavaBinarySerializer implements ISerializer
{

    /**
     * Serializes data with using ObjectOutputStream.
     */
    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream aSerializedData = new ByteArrayOutputStream();
            ObjectOutputStream aWriter = new ObjectOutputStream(aSerializedData);
            aWriter.writeObject(dataToSerialize);
            
            return aSerializedData.toByteArray();
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "failed to serialize object.", err);
            throw err;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Deserializes data with using ObjectInputStream.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(Object serializedData, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayInputStream aSerializedData = new ByteArrayInputStream((byte[]) serializedData);
            ObjectInputStream aReader = new ObjectInputStream(aSerializedData);
            Object aResult = aReader.readObject();
            return (T)aResult;
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "failed to deserialize data.", err);
            throw err;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private String TracedObject()
    {
        return "JavaBinarySerializer ";
    }
}
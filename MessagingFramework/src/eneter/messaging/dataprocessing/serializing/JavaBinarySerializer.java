package eneter.messaging.dataprocessing.serializing;

import java.io.*;

import eneter.messaging.diagnostic.EneterTrace;

public class JavaBinarySerializer implements ISerializer
{

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
        catch (Error err)
        {
            EneterTrace.error(TracedObject() + "failed to serialize object.", err);
            throw err;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

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
        catch (Error err)
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
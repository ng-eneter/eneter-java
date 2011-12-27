package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.*;

import org.junit.*;

public class Test_AesSerializer
{
    @Test
    public void SerializeDeserialize() throws Exception
    {
        Object[] aDataToSerialize = {(int)10, "Hello"};
        
        AesSerializer aSerializer = new AesSerializer("MyPassword");
        Object aSerializedData = aSerializer.serialize(aDataToSerialize, Object[].class);
        
        Object[] aDeserializedData = aSerializer.deserialize(aSerializedData, Object[].class);
        
        assertEquals(2, aDeserializedData.length);
        assertEquals((int)10, aDeserializedData[0]);
        assertEquals("Hello", aDeserializedData[1]);
    }
    
}

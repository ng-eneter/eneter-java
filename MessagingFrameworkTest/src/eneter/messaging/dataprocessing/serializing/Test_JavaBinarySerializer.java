package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.*;

import java.io.Serializable;

import org.junit.Test;

public class Test_JavaBinarySerializer
{
    public static class TestClass implements Serializable
    {
        private static final long serialVersionUID = -4342650306845017426L;
        public int myNumber;
        public String myString;
    }
    
    @Test
    public void serializeDeserialize() throws Exception
    {
        String sData = "Hello";
        
        ISerializer aSerializer = new JavaBinarySerializer();
        Object aSerializedData = aSerializer.serialize(sData, String.class);
        
        String aDeserializedData = aSerializer.deserialize(aSerializedData, String.class);
        
        assertEquals(sData, aDeserializedData);
    }
    
    @Test
    public void serializeDeserializeClass() throws Exception
    {
        TestClass aData = new TestClass();
        aData.myNumber = 100;
        aData.myString = "Hello";
        
        ISerializer aSerializer = new JavaBinarySerializer();
        Object aSerializedData = aSerializer.serialize(aData, TestClass.class);
        
        TestClass aDeserializedData = aSerializer.deserialize(aSerializedData, TestClass.class);
        
        assertEquals(aData.myNumber, aDeserializedData.myNumber);
        assertEquals(aData.myString, aDeserializedData.myString);
    }
}

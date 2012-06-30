package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.*;

import org.junit.*;

public class Test_GZipSerializer
{
    @Test
    public void SerializeLongStringWithXml() throws Exception
    {
        ISerializer aTestedSerializer = new GZipSerializer(new XmlStringSerializer());
        
        StringBuilder aStringBuilder = new StringBuilder(32100);
        for (int i = 0; i < 32000; ++i)
        {
            aStringBuilder.append('A');
        }
        
        String aLongString = aStringBuilder.toString();
        
        byte[] aSerializedData = (byte[])aTestedSerializer.serialize(aLongString, String.class);
        
        String aDeserializedLongString = aTestedSerializer.deserialize(aSerializedData, String.class);
        
        assertEquals(aLongString, aDeserializedLongString);
    }
    
    @Test
    public void SerializeLongStringWithBin() throws Exception
    {
        ISerializer aTestedSerializer = new GZipSerializer(new JavaBinarySerializer());
        
        StringBuilder aStringBuilder = new StringBuilder(32100);
        for (int i = 0; i < 32000; ++i)
        {
            aStringBuilder.append('A');
        }
        
        String aLongString = aStringBuilder.toString();
        
        byte[] aSerializedData = (byte[])aTestedSerializer.serialize(aLongString, String.class);
        
        String aDeserializedLongString = aTestedSerializer.deserialize(aSerializedData, String.class);
        
        assertEquals(aLongString, aDeserializedLongString);
    }
    
    
}

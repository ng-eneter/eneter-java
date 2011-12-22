package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.*;

import org.junit.*;

public class Test_GZipSerializer
{
    @Before
    public void setup()
    {
        myTestedSerializer = new GZipSerializer(new XmlStringSerializer());
    }
    
    @Test
    public void SerializeLongString() throws Exception
    {
        StringBuilder aStringBuilder = new StringBuilder(32100);
        for (int i = 0; i < 32000; ++i)
        {
            aStringBuilder.append('A');
        }
        
        String aLongString = aStringBuilder.toString();
        
        byte[] aSerializedData = (byte[])myTestedSerializer.serialize(aLongString, String.class);
        
        String aDeserializedLongString = myTestedSerializer.deserialize(aSerializedData, String.class);
        
        assertEquals(aLongString, aDeserializedLongString);
    }
    
    
    private ISerializer myTestedSerializer;
}

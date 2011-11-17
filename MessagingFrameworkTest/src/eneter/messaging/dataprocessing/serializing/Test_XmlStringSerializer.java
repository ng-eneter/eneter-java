package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.assertEquals;

import org.junit.*;

public class Test_XmlStringSerializer extends SerializerTesterBase
{
    @Before
    public void Setup()
    {
        TestedSerializer = new XmlStringSerializer();
        //TestedSerializer2 = new XmlStringSerializer();
    }
    
    @Test
    public void serializeXmlKeywords() throws Exception
    {
        String s = "& < > \" '";
        Object aSerializedData = TestedSerializer.serialize(s, String.class);
        assertEquals("<String>&amp; &lt; &gt; &quot; &apos;</String>", (String)aSerializedData);
        
        String aDeserializedData = TestedSerializer.deserialize(aSerializedData, String.class);
        assertEquals(s, aDeserializedData);
    }
    
    @Test
    public void serializeChar() throws Exception
    {
        char c = 'A';
        Object aSerializedData = TestedSerializer.serialize(c, char.class);
        assertEquals("<char>65</char>", (String)aSerializedData);
        
        char aDeserializedData = TestedSerializer.deserialize(aSerializedData, char.class);
        assertEquals(c, aDeserializedData);
    }
}

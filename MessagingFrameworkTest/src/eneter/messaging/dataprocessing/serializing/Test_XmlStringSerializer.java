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
    public void SerializeSpecialXmlCharacters() throws Exception
    {
        String aData = "& < > \" '";
        Object aSerializedData = TestedSerializer.serialize(aData, String.class);
        //String aDeserializedData = TestedSerializer.Deserialize<String>(aSerializedData);

        //Assert.AreEqual(aData, aDeserializedData);
        
        assertEquals("<String>&amp; &lt; &gt; &quot; &apos;</String>", (String)aSerializedData);
    }
}

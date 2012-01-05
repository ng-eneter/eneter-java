package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.*;


public class Test_XmlStringSerializer
{
    public static class MyTestClass1
    {
        public int k = 100;
        public String str = "Hello";
    }
    
    public static class MyTestClass2
    {
        public int kk = 111;
        public MyTestClass1 vv = new MyTestClass1();
        public int mm = 222;
    }
    
    public static enum MyEnum
    {
        Monday,
        Tuesday,
        Wednwsday
    }
    
    public static class MyTestClass3
    {
        public MyEnum myEnum = MyEnum.Monday;
    }
    
    public static class MyClassWithObject
    {
        public Object myItem;
    }
    
    
    @Before
    public void Setup()
    {
        TestedSerializer = new XmlStringSerializer();
        //TestedSerializer2 = new XmlStringSerializer();
    }
    
    @Test
    public void serializeDeserialize() throws Exception
    {
        String aData = "hello world";
        Object aSerializedData = TestedSerializer.serialize(aData, String.class);
        
        assertEquals("<String xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">hello world</String>", (String)aSerializedData);
        
        String aDeserializedData = TestedSerializer.deserialize(aSerializedData, String.class);

        assertEquals(aData, aDeserializedData);
    }
    
    @Test
    public void serializeInt() throws Exception
    {
        int a = 10;
        Object aSerializedData = TestedSerializer.serialize(a, int.class);
        
        assertEquals("<int xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">10</int>", (String)aSerializedData);
        
        int aDeserializedData = TestedSerializer.deserialize(aSerializedData, int.class);

        assertEquals(a, aDeserializedData);
    }
    
    @Test
    public void serializeArray() throws Exception
    {
        int[] a = {1,2,3};
        Object aSerializedData = TestedSerializer.serialize(a, int[].class);
        
        assertEquals("<ArrayOfInt xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><int>1</int><int>2</int><int>3</int></ArrayOfInt>", (String)aSerializedData);
        
        int[] aDeserializedData = TestedSerializer.deserialize(aSerializedData, int[].class);

        assertTrue(Arrays.equals(a, aDeserializedData));
    }
    
    @Test
    public void serializeByteArray() throws Exception
    {
        byte[] a = {0, 1, 2, 3, (byte)255};
        Object aSerializedData = TestedSerializer.serialize(a, byte[].class);
        
        assertEquals("<base64Binary xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">AAECA/8=</base64Binary>", (String)aSerializedData);
        
        byte[] aDeserializedData = TestedSerializer.deserialize(aSerializedData, byte[].class);

        assertTrue(Arrays.equals(a, aDeserializedData));
        
        // Serialize empty byte array.
        byte[] aa = {};
        aSerializedData = TestedSerializer.serialize(aa, byte[].class);
        
        assertEquals("<base64Binary xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"></base64Binary>", (String)aSerializedData);
        
        aDeserializedData = TestedSerializer.deserialize(aSerializedData, byte[].class);

        assertEquals(0, aDeserializedData.length);
        assertTrue(Arrays.equals(aa, aDeserializedData));
        
        // Serialize null byte array.
        aSerializedData = TestedSerializer.serialize(null, byte[].class);
        
        assertEquals("<base64Binary xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xsi:nil=\"true\"/>", (String)aSerializedData);
        
        aDeserializedData = TestedSerializer.deserialize(aSerializedData, byte[].class);

        assertNull(aDeserializedData);
    }
    
    @Test
    public void serializeObjectArray() throws Exception
    {
        Object[] a = {(int)1,"Hello",(char)'A'};
        Object aSerializedData = TestedSerializer.serialize(a, Object[].class);
        
        assertEquals("<ArrayOfAnyType xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><anyType xsi:type=\"xsd:int\">1</anyType><anyType xsi:type=\"xsd:string\">Hello</anyType><anyType xmlns:q1=\"http://microsoft.com/wsdl/types/\" xsi:type=\"q1:char\">65</anyType></ArrayOfAnyType>", (String)aSerializedData);
        
        Object[] aDeserializedData = TestedSerializer.deserialize(aSerializedData, Object[].class);

        assertEquals(3, aDeserializedData.length);
        assertEquals(Integer.class, aDeserializedData[0].getClass());
        assertEquals(1, aDeserializedData[0]);
        assertEquals(String.class, aDeserializedData[1].getClass());
        assertEquals("Hello", aDeserializedData[1]);
        assertEquals(Character.class, aDeserializedData[2].getClass());
        assertEquals('A', aDeserializedData[2]);
    }
    
    @Test
    public void serializeArrayOfCustomClass() throws Exception
    {
        MyTestClass1[] aClasses = { new MyTestClass1(), new MyTestClass1(), new MyTestClass1() };
        aClasses[0].k = 1;
        aClasses[1].k = 2;
        aClasses[2].k = 3;
        
        Object aSerializedData = TestedSerializer.serialize(aClasses, MyTestClass1[].class);
        
        assertEquals("<ArrayOfMyTestClass1 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><MyTestClass1><k>1</k><str>Hello</str></MyTestClass1><MyTestClass1><k>2</k><str>Hello</str></MyTestClass1><MyTestClass1><k>3</k><str>Hello</str></MyTestClass1></ArrayOfMyTestClass1>", (String)aSerializedData);
        
        MyTestClass1[] aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass1[].class);

        assertEquals(aClasses.length, aDeserializedData.length);
        assertEquals(aClasses[0].k, aDeserializedData[0].k);
        assertEquals(aClasses[0].str, aDeserializedData[0].str);
        assertEquals(aClasses[1].k, aDeserializedData[1].k);
        assertEquals(aClasses[1].str, aDeserializedData[1].str);
        assertEquals(aClasses[2].k, aDeserializedData[2].k);
        assertEquals(aClasses[2].str, aDeserializedData[2].str);
    }
    
    
    @Test
    public void serializeClass() throws Exception
    {
        MyTestClass1 aClass = new MyTestClass1();
        aClass.k = -10;
        aClass.str = "Eneter";
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass1.class);
        
        assertEquals("<MyTestClass1 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><k>-10</k><str>Eneter</str></MyTestClass1>", (String)aSerializedData);
        
        MyTestClass1 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass1.class);
        
        assertEquals(aClass.k, aDeserializedData.k);
        assertEquals(aClass.str, aDeserializedData.str);
    }
    
    @Test
    public void serializeCompositeClass() throws Exception
    {
        MyTestClass2 aClass = new MyTestClass2();
        aClass.kk = 1000;
        aClass.vv.k = 5;
        aClass.vv.str = "Eneter";
        aClass.mm = 2000;
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><kk>1000</kk><vv><k>5</k><str>Eneter</str></vv><mm>2000</mm></MyTestClass2>", (String)aSerializedData);
        
        MyTestClass2 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass2.class);
        
        assertEquals(aClass.kk, aDeserializedData.kk);
        assertEquals(aClass.vv.k, aDeserializedData.vv.k);
        assertEquals(aClass.vv.str, aDeserializedData.vv.str);
        assertEquals(aClass.mm, aDeserializedData.mm);
    }
    
    @Test
    public void serializeNull() throws Exception
    {
        MyTestClass2 aClass = null;
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xsi:nil=\"true\"/>", (String)aSerializedData);
        
        MyTestClass2 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass2.class);
        
        assertNull(aDeserializedData);
    }
    
    @Test
    public void serializeClassWithNullField() throws Exception
    {
        MyTestClass2 aClass = new MyTestClass2();
        aClass.vv = null;
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><kk>111</kk><vv xsi:nil=\"true\"/><mm>222</mm></MyTestClass2>", (String)aSerializedData);
        
        MyTestClass2 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass2.class);
        
        assertEquals(aClass.kk, aDeserializedData.kk);
        assertNull(aClass.vv);
    }
    
    @Test
    public void serializeEnum() throws Exception
    {
        MyEnum aData = MyEnum.Tuesday;
        
        Object aSerializedData = TestedSerializer.serialize(aData, MyEnum.class);
        assertEquals("<MyEnum xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">Tuesday</MyEnum>", (String)aSerializedData);

        MyEnum aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyEnum.class);
        assertTrue(aData == aDeserializedData);
    }
    
    @Test
    public void serializeClassWithEnum() throws Exception
    {
        MyTestClass3 aTestClass = new MyTestClass3();
        aTestClass.myEnum = MyEnum.Tuesday;
        
        Object aSerializedData = TestedSerializer.serialize(aTestClass, MyTestClass3.class);
        assertEquals("<MyTestClass3 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><myEnum>Tuesday</myEnum></MyTestClass3>", (String)aSerializedData);

        MyTestClass3 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass3.class);
        assertEquals(aTestClass.myEnum, aDeserializedData.myEnum);
    }
    
    @Test
    public void serializeClassWithObjectField_String() throws Exception
    {
        MyClassWithObject aClass = new MyClassWithObject();
        aClass.myItem = "Hello"; // assign string to the object
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyClassWithObject.class);
        
        assertEquals("<MyClassWithObject xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><myItem xsi:type=\"xsd:string\">Hello</myItem></MyClassWithObject>", (String)aSerializedData);
        
        MyClassWithObject aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyClassWithObject.class);
        
        assertEquals(aClass.myItem, aDeserializedData.myItem);
    }
    
    @Test
    public void serializeClassWithObjectField_ByteArray() throws Exception
    {
        MyClassWithObject aClass = new MyClassWithObject();
        aClass.myItem = new byte[] {0, 1, 2, 3, (byte)255}; // assign string to the object
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyClassWithObject.class);
        
        assertEquals("<MyClassWithObject xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><myItem xsi:type=\"xsd:base64Binary\">AAECA/8=</myItem></MyClassWithObject>", (String)aSerializedData);
        
        MyClassWithObject aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyClassWithObject.class);
        
        assertEquals(5, ((byte[])aDeserializedData.myItem).length);
        assertEquals(0, ((byte[])aDeserializedData.myItem)[0]);
        assertEquals(1, ((byte[])aDeserializedData.myItem)[1]);
        assertEquals(2, ((byte[])aDeserializedData.myItem)[2]);
        assertEquals(3, ((byte[])aDeserializedData.myItem)[3]);
        assertEquals((byte)255, ((byte[])aDeserializedData.myItem)[4]);
    }
    
    
    @Test
    public void serializeXmlKeywords() throws Exception
    {
        String s = "& < > \" '";
        Object aSerializedData = TestedSerializer.serialize(s, String.class);
        assertEquals("<String xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">&amp; &lt; &gt; &quot; &apos;</String>", (String)aSerializedData);
        
        String aDeserializedData = TestedSerializer.deserialize(aSerializedData, String.class);
        assertEquals(s, aDeserializedData);
    }
    
    @Test
    public void serializeChar() throws Exception
    {
        char c = 'A';
        Object aSerializedData = TestedSerializer.serialize(c, char.class);
        assertEquals("<char xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">65</char>", (String)aSerializedData);
        
        char aDeserializedData = TestedSerializer.deserialize(aSerializedData, char.class);
        assertEquals(c, aDeserializedData);
    }
    
    protected ISerializer TestedSerializer;
}

package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.*;

public abstract class SerializerTesterBase
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
    
    
    
    @Test
    public void serializeDeserialize() throws Exception
    {
        String aData = "hello world";
        Object aSerializedData = TestedSerializer.serialize(aData, String.class);
        
        assertEquals("<String xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">hello world</String>", (String)aSerializedData);
        
        String aDeserializedData = TestedSerializer.deserialize(aSerializedData, String.class);

        assertEquals(aData, aDeserializedData);
    }
    
    @Test
    public void serializeInt() throws Exception
    {
        int a = 10;
        Object aSerializedData = TestedSerializer.serialize(a, int.class);
        
        assertEquals("<int xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">10</int>", (String)aSerializedData);
        
        int aDeserializedData = TestedSerializer.deserialize(aSerializedData, int.class);

        assertEquals(a, aDeserializedData);
    }
    
    @Test
    public void serializeArray() throws Exception
    {
        int[] a = {1,2,3};
        Object aSerializedData = TestedSerializer.serialize(a, int[].class);
        
        assertEquals("<ArrayOfInt xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><int>1</int><int>2</int><int>3</int></ArrayOfInt>", (String)aSerializedData);
        
        int[] aDeserializedData = TestedSerializer.deserialize(aSerializedData, int[].class);

        assertTrue(Arrays.equals(a, aDeserializedData));
    }
    
    @Test
    public void serializeArrayOfCustomClass() throws Exception
    {
        MyTestClass1[] aClasses = { new MyTestClass1(), new MyTestClass1(), new MyTestClass1() };
        aClasses[0].k = 1;
        aClasses[1].k = 2;
        aClasses[2].k = 3;
        
        Object aSerializedData = TestedSerializer.serialize(aClasses, MyTestClass1[].class);
        
        assertEquals("<ArrayOfMyTestClass1 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><MyTestClass1><k>1</k><str>Hello</str></MyTestClass1><MyTestClass1><k>2</k><str>Hello</str></MyTestClass1><MyTestClass1><k>3</k><str>Hello</str></MyTestClass1></ArrayOfMyTestClass1>", (String)aSerializedData);
        
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
        
        assertEquals("<MyTestClass1 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><k>-10</k><str>Eneter</str></MyTestClass1>", (String)aSerializedData);
        
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
        
        assertEquals("<MyTestClass2 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><kk>1000</kk><vv><k>5</k><str>Eneter</str></vv><mm>2000</mm></MyTestClass2>", (String)aSerializedData);
        
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
        
        assertEquals("<MyTestClass2 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"/>", (String)aSerializedData);
        
        MyTestClass2 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass2.class);
        
        assertNull(aDeserializedData);
    }
    
    @Test
    public void serializeClassWithNullField() throws Exception
    {
        MyTestClass2 aClass = new MyTestClass2();
        aClass.vv = null;
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><kk>111</kk><vv xsi:nil=\"true\"/><mm>222</mm></MyTestClass2>", (String)aSerializedData);
        
        MyTestClass2 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass2.class);
        
        assertEquals(aClass.kk, aDeserializedData.kk);
        assertNull(aClass.vv);
    }
    
    @Test
    public void serializeEnum() throws Exception
    {
        MyEnum aData = MyEnum.Tuesday;
        
        Object aSerializedData = TestedSerializer.serialize(aData, MyEnum.class);
        assertEquals("<MyEnum xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">Tuesday</MyEnum>", (String)aSerializedData);

        MyEnum aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyEnum.class);
        assertTrue(aData == aDeserializedData);
    }
    
    @Test
    public void serializeClassWithEnum() throws Exception
    {
        MyTestClass3 aTestClass = new MyTestClass3();
        aTestClass.myEnum = MyEnum.Tuesday;
        
        Object aSerializedData = TestedSerializer.serialize(aTestClass, MyTestClass3.class);
        assertEquals("<MyTestClass3 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><myEnum>Tuesday</myEnum></MyTestClass3>", (String)aSerializedData);

        MyTestClass3 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass3.class);
        assertEquals(aTestClass.myEnum, aDeserializedData.myEnum);
    }
    
    @Test
    public void serializeClassWithObjectField() throws Exception
    {
        MyClassWithObject aClass = new MyClassWithObject();
        aClass.myItem = "Hello"; // assign string to the object
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyClassWithObject.class);
        
        assertEquals("<MyClassWithObject xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><myItem xsi:type=\"xsd:string\">Hello</myItem></MyClassWithObject>", (String)aSerializedData);
        
        MyClassWithObject aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyClassWithObject.class);
        
        assertEquals(aClass.myItem, aDeserializedData.myItem);
    }
    
    protected ISerializer TestedSerializer;
}

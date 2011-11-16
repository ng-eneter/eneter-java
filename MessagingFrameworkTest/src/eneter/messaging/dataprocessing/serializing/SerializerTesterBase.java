package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.*;

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
    }
    
    public static class MyGenericClass<T>
    {
        public T myItem;
    }
    
    
    @Test
    public void serializeDeserialize() throws Exception
    {
        String aData = "hello world";
        Object aSerializedData = TestedSerializer.serialize(aData, String.class);
        
        assertEquals("<String>hello world</String>", (String)aSerializedData);
        
        String aDeserializedData = TestedSerializer.deserialize(aSerializedData, String.class);

        assertEquals(aData, aDeserializedData);
    }
    
    @Test
    public void serializeInt() throws Exception
    {
        int a = 10;
        Object aSerializedData = TestedSerializer.serialize(a, int.class);
        
        assertEquals("<int>10</int>", (String)aSerializedData);
        
        int aDeserializedData = TestedSerializer.deserialize(aSerializedData, int.class);

        assertEquals(a, aDeserializedData);
    }
    
    @Test
    public void serializeArray() throws Exception
    {
        int[] a = {1,2,3};
        Object aSerializedData = TestedSerializer.serialize(a, int[].class);
        
        assertEquals("<ArrayOfInt><int>1</int><int>2</int><int>3</int></ArrayOfInt>", (String)aSerializedData);
        
        int[] aDeserializedData = TestedSerializer.deserialize(aSerializedData, int[].class);

        assertTrue(Arrays.equals(a, aDeserializedData));
    }
    
    @Test
    public void serializeClass() throws Exception
    {
        MyTestClass1 aClass = new MyTestClass1();
        aClass.k = -10;
        aClass.str = "Eneter";
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass1.class);
        
        assertEquals("<MyTestClass1><k>-10</k><str>Eneter</str></MyTestClass1>", (String)aSerializedData);
        
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
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2><kk>1000</kk><vv><k>5</k><str>Eneter</str></vv></MyTestClass2>", (String)aSerializedData);
        
        MyTestClass2 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass2.class);
        
        assertEquals(aClass.kk, aDeserializedData.kk);
        assertEquals(aClass.vv.k, aDeserializedData.vv.k);
        assertEquals(aClass.vv.str, aDeserializedData.vv.str);
    }
    
    @Test
    public void serializeNull() throws Exception
    {
        MyTestClass2 aClass = null;
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2></MyTestClass2>", (String)aSerializedData);
        
        MyTestClass2 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass2.class);
        
        assertNull(aDeserializedData);
    }
    
    @Test
    public void serializeClassWithNullField() throws Exception
    {
        MyTestClass2 aClass = new MyTestClass2();
        aClass.vv = null;
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2><kk>111</kk><vv></vv></MyTestClass2>", (String)aSerializedData);
        
        MyTestClass2 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass2.class);
        
        assertEquals(aClass.kk, aDeserializedData.kk);
        assertNull(aClass.vv);
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
    
    protected ISerializer TestedSerializer;
}

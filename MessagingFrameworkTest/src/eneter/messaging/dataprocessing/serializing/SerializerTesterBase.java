package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.*;

public abstract class SerializerTesterBase
{
    private class MyTestClass1
    {
        @SuppressWarnings("unused")
        public int k = 100;
        
        @SuppressWarnings("unused")
        public String str = "Hello";
    }
    
    private class MyTestClass2
    {
        @SuppressWarnings("unused")
        public int kk = 111;
        
        @SuppressWarnings("unused")
        public MyTestClass1 vv = new MyTestClass1();
    }
    
    
    @Test
    public void SerializeDeserialize() throws Exception
    {
        String aData = "hello world";
        Object aSerializedData = TestedSerializer.serialize(aData, String.class);
        
        assertEquals("<String>hello world</String>", (String)aSerializedData);
        
        String aDeserializedData = TestedSerializer.deserialize(aSerializedData, String.class);

        assertEquals(aData, aDeserializedData);
    }
    
    @Test
    public void SerializeInt() throws Exception
    {
        int a = 10;
        Object aSerializedData = TestedSerializer.serialize(a, int.class);
        
        assertEquals("<int>10</int>", (String)aSerializedData);
        
        int aDeserializedData = TestedSerializer.deserialize(aSerializedData, int.class);

        assertEquals(a, aDeserializedData);
    }
    
    @Test
    public void SerializeArray() throws Exception
    {
        int[] a = {1,2,3};
        Object aSerializedData = TestedSerializer.serialize(a, int[].class);
        
        assertEquals("<int[]><int>1</int><int>2</int><int>3</int></int[]>", (String)aSerializedData);
        
        int[] aDeserializedData = TestedSerializer.deserialize(aSerializedData, int[].class);

        assertTrue(Arrays.equals(a, aDeserializedData));
    }
    
    @Test
    public void SerializeClass() throws Exception
    {
        MyTestClass1 aClass = new MyTestClass1();
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass1.class);
        
        assertEquals("<MyTestClass1><k>100</k><str>Hello</str></MyTestClass1>", (String)aSerializedData);
        
        //MyTestClass1 aDeserializedData = TestedSerializer.deserialize(aSerializedData, MyTestClass1.class);
        
        //assertEquals(aClass.k, aDeserializedData.k);
        //assertEquals(aClass.str, aDeserializedData.str);
    }
    
    @Test
    public void SerializeCompositeClass() throws Exception
    {
        MyTestClass2 aClass = new MyTestClass2();
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2><kk>111</kk><vv><k>100</k><str>Hello</str></vv></MyTestClass2>", (String)aSerializedData);
        
        
    }
    
    @Test
    public void SerializeNull() throws Exception
    {
        MyTestClass2 aClass = null;
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2></MyTestClass2>", (String)aSerializedData);
    }
    
    @Test
    public void SerializeClassWithNullField() throws Exception
    {
        MyTestClass2 aClass = new MyTestClass2();
        aClass.vv = null;
        
        Object aSerializedData = TestedSerializer.serialize(aClass, MyTestClass2.class);
        
        assertEquals("<MyTestClass2><kk>111</kk><vv></vv></MyTestClass2>", (String)aSerializedData);
    }
    
    protected ISerializer TestedSerializer;
}

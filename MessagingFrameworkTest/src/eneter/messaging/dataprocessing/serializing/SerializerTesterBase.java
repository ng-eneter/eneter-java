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
    
    public static class MyGenericClass<T>
    {
        public T myItem;
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
    
    //@Test
    public void serializeGenericClass() throws Exception
    {
        MyGenericClass<Double> aTestClass = new MyGenericClass<Double>();
        aTestClass.myItem = 10.0;
        
        Object aSerializedData = TestedSerializer.serialize(aTestClass, MyGenericClass.class);
        assertEquals("<MyGenericClass xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><myItem>10.0</myItem></MyGenericClass>", (String)aSerializedData);

        MyGenericClass<String> aReferenceClass = new MyGenericClass<String>();
        aReferenceClass.myItem = "";
        MyGenericClass<String> aDeserializedData = TestedSerializer.deserialize(aSerializedData, aReferenceClass.getClass());
        assertEquals(aTestClass.myItem, aDeserializedData.myItem);
    }
    
    
    protected ISerializer TestedSerializer;
}

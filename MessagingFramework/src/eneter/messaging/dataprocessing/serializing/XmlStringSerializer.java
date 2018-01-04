/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

import java.lang.reflect.*;
import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Serializes data to XML which is compatible with .NET.
 * 
 * This is the default serializer used by the framework. The serializer is compatible
 * with XmlStringSerializer from Eneter Messaging Framework for .NET.
 * Therefore, you can use it for the communication between Java and .NET applications.
 * <b>The serializer does not support generic types on Java and Android platforms!</b><br/>
 */
public class XmlStringSerializer implements ISerializer
{
    public XmlStringSerializer()
    {
        int i=0;
        for (char c='A'; c<='Z'; c++) map1[i++] = c;
        for (char c='a'; c<='z'; c++) map1[i++] = c;
        for (char c='0'; c<='9'; c++) map1[i++] = c;
        map1[i++] = '+'; map1[i++] = '/';
        
        for (i=0; i<map2.length; i++) map2[i] = -1;
        for (i=0; i<64; i++) map2[map1[i]] = (byte)i;
    }
    
    /**
     * Serializes data to the xml string.
     */
    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: The buffer size in StringBuilder is 16 bytes by default.
            // Serialized structures will need more.
            // So to avoid allocations initialize it with 500.
            StringBuilder aSerializedObjectStr = new StringBuilder(500);

            // Get the root name compatible with .Net
            String aRootName = getElementName(clazz);
            
            // Note: keep the space character at the beginning of the name-space string!!!
            String aNameSpacesAndAttributes = " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" + getAttributes(dataToSerialize, clazz);
            
            serializeElement(aRootName, aNameSpacesAndAttributes, dataToSerialize, aSerializedObjectStr);

            return aSerializedObjectStr.toString();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Deserializes data into the specified type.
     */
    @Override
    public <T> T deserialize(Object serializedData, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (serializedData == null)
            {
                String anErrorMsg = "The input parameter 'serializedData' is null.";
                EneterTrace.error(anErrorMsg);
                throw new IllegalStateException(anErrorMsg);
            }
            
            if (serializedData instanceof String == false)
            {
                String anErrorMsg = "Input parameter 'serializedData' is not String.";
                EneterTrace.error(anErrorMsg);
                throw new IllegalStateException(anErrorMsg);
            }
    
            // Create browser to read the xml from the string.
            XmlDataBrowser anXmlBrowser = new XmlDataBrowser((String) serializedData);
    
            // Get the root element.
            XmlDataBrowser.TElement aRootElement = anXmlBrowser.getElement(0);
            
            // Start the deserialization.
            T aDeserializedObject = deserializeElement(anXmlBrowser, aRootElement, clazz);
    
            return aDeserializedObject;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void serializeElement(String xmlElementName, String attributeSection, Object dataToSerialize, StringBuilder xmlResult)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            xmlResult.append("<");
            xmlResult.append(xmlElementName);
            xmlResult.append(attributeSection);
            
            if (dataToSerialize == null)
            {
                // If the serialized item is null, then close the element.
                // Note: Attribute indicating null is already put into the attributes. 
                xmlResult.append("/>");
                return;
            }
            
            xmlResult.append(">");
    
            Class<?> aSerializedType = dataToSerialize.getClass();
            
            // Generic types are not supported. :-(
            if (aSerializedType.getTypeParameters().length > 0)
            {
                String anErrorMsg = "The XmlStringSerializer does not support generic types.";
                EneterTrace.error(anErrorMsg);
                throw new IllegalStateException(anErrorMsg);
            }
            
            // If it is string then special characters must be replaced.
            if (dataToSerialize instanceof String)
            {
                serializeString((String) dataToSerialize, xmlResult);
            }
            // If it is a primitive type
            else if (aSerializedType.isPrimitive() ||
                     aSerializedType == Boolean.class ||
                     aSerializedType == Byte.class ||
                     aSerializedType == Character.class ||
                     aSerializedType == Short.class ||
                     aSerializedType == Integer.class ||
                     aSerializedType == Long.class ||
                     aSerializedType == Float.class ||
                     aSerializedType == Double.class)
            {
                serializePrimitiveType(dataToSerialize, xmlResult);
            }
            else if (aSerializedType.isArray())
            {
                serializeArray(dataToSerialize, xmlResult);
            }
            else if (aSerializedType.isEnum())
            {
                xmlResult.append(dataToSerialize.toString());
            }
            // If it is a class with public members.
            else
            {
                if (aSerializedType.getFields().length > 0)
                {
                    // If the object has public members then serialize them.
                    Field[] aPublicFields = aSerializedType.getFields();
                    for (Field aField : aPublicFields)
                    {
                        // If the field is not an artificial field created by the compiler.
                        if (!aField.isSynthetic())
                        {
                            Object aToSerialize = aField.get(dataToSerialize);
                            String anAttributes = getAttributes(aToSerialize, aField.getType());
                            serializeElement(aField.getName(), anAttributes, aToSerialize, xmlResult);
                        }
                    }
                }
            }
    
            xmlResult.append("</");
            xmlResult.append(xmlElementName);
            xmlResult.append(">");
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T deserializeElement(XmlDataBrowser xmlBrowser, XmlDataBrowser.TElement xmlElement, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // If the deserialized value is null.
            if (xmlElement.myIsNull)
            {
                return null;
            }
            
            if (clazz.getTypeParameters().length > 0)
            {
                String anErrorMsg = "The XmlStringSerializer does not support generic types.";
                EneterTrace.error(anErrorMsg);
                throw new IllegalStateException(anErrorMsg);
            }
            
    
            // If it is a simple type (not a custom class) then the field contains the value.
            if (clazz.isPrimitive() ||
                clazz == Boolean.class ||
                clazz == Byte.class ||
                clazz == Character.class ||
                clazz == Short.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Float.class ||
                clazz == Double.class)
            {
                return deserializePrimitiveType(xmlBrowser, xmlElement.myValueStartPosition, xmlElement.myValueLength, clazz);
            }
            else if (clazz == String.class)
            {
                String aValue = xmlBrowser.getStringValue(xmlElement.myValueStartPosition, xmlElement.myValueLength);
                return (T) aValue;
            }
            // If it is an array
            else if (clazz.isArray())
            {
                return deserializeArray(xmlBrowser, xmlElement.myValueStartPosition, xmlElement.myValueLength, clazz);
            }
            // If it is an enum.
            else if (clazz.isEnum())
            {
                // Get the enum value from the xml.
                String aValue = xmlBrowser.getStringValue(xmlElement.myValueStartPosition, xmlElement.myValueLength);
                
                // Iterate via all possible enum values and find matching one.
                for (T t : clazz.getEnumConstants())
                {
                    if (aValue.equals(t.toString()))
                    {
                        return t;
                    }
                }
                
                String anErrorMsg = "Uknown enum value found during the deserialization.";
                EneterTrace.error(anErrorMsg);
                throw new IllegalStateException(anErrorMsg);
            }
            // If it is the Object but the type is declared explicitly in the attribute
            else if (xmlElement.myClazz != null && clazz == Object.class)
            {
                return (T) deserializeElement(xmlBrowser, xmlElement, xmlElement.myClazz);
            }
            // Ok, then it is some class
            else
            {
                // Get value of the element.
                ArrayList<XmlDataBrowser.TElement> anElements = xmlBrowser.getElements(xmlElement.myValueStartPosition, xmlElement.myValueLength);
    
                // Create the instance of deserialized object via default constructor.
                T aDeserializedObject = (T) clazz.newInstance();
    
                // Get public fields of the deserialized object.
                Field[] aPublicFields = clazz.getFields();
    
                // Go through fields and deserialize them.
                int aSearchIdx = 0;
                for (Field aField : aPublicFields)
                {
                    // If it is not a field created by compiler.
                    if (!aField.isSynthetic())
                    {
                        String aFieldName = aField.getName();
        
                        // Find the element containing the value.
                        // Note: To avoid the looping complexity - searching again and
                        // again from the very beginning,
                        // the loop behaves as cycle. The last position is remembered
                        // and the next search starts from this position.
                        XmlDataBrowser.TElement anElement = null;
                        for (int aSearchedLength = 0; aSearchedLength < anElements.size(); ++aSearchedLength)
                        {
                            XmlDataBrowser.TElement anTmpElement = anElements.get(aSearchIdx);
        
                            if (anTmpElement.myName.equals(aFieldName))
                            {
                                // Store found element.
                                anElement = anTmpElement;
        
                                // Next search start from the next position.
                                ++aSearchIdx;
        
                                // If we are at the end, then the next search will start
                                // from the beginning.
                                if (aSearchIdx == anElements.size())
                                {
                                    aSearchIdx = 0;
                                }
        
                                break;
                            }
        
                            ++aSearchIdx;
        
                            // If we are at the end, then start from the beginning.
                            if (aSearchIdx == anElements.size())
                            {
                                aSearchIdx = 0;
                            }
                        }
        
                        // If the element was found then deserialize it.
                        // Note: If the element is not found ignore it and keep there the default value.
                        //       It will be null for referenced types. .NET has the same behaviour.
                        if (anElement != null)
                        {
                            // Recursively deserialize the object for the field.
                            Object aValue = deserializeElement(xmlBrowser, anElement, aField.getType());
            
                            // Set the created object to the field.
                            aDeserializedObject.getClass().getField(aFieldName).set(aDeserializedObject, aValue);
                        }
                    }
                }
    
                return aDeserializedObject;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
        


    private void serializeString(String s, StringBuilder xmlResult)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            for (int i = 0; i < s.length(); ++i)
            {
                char aCh = s.charAt(i);
    
                switch (aCh)
                {
                case '&':
                {
                    xmlResult.append("&amp;");
                    break;
                }
                case '<':
                {
                    xmlResult.append("&lt;");
                    break;
                }
                case '>':
                {
                    xmlResult.append("&gt;");
                    break;
                }
                case '"':
                {
                    xmlResult.append("&quot;");
                    break;
                }
                case '\'':
                {
                    xmlResult.append("&apos;");
                    break;
                }
                default:
                {
                    xmlResult.append(aCh);
                }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void serializePrimitiveType(Object dataToSerialize, StringBuilder xmlResult)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // If it is a character, then write the numeric code to the xml
            if (dataToSerialize instanceof Character)
            {
                int aCharValue = (int)((Character)dataToSerialize);
                xmlResult.append(String.valueOf(aCharValue));
            }
            // If it is a wrapper of a primitive type then write its value.
            else if (dataToSerialize instanceof Boolean
                    || dataToSerialize instanceof Byte
                    || dataToSerialize instanceof Double
                    || dataToSerialize instanceof Float
                    || dataToSerialize instanceof Integer
                    || dataToSerialize instanceof Long
                    || dataToSerialize instanceof Short)
            {
                xmlResult.append(dataToSerialize.toString());
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void serializeArray(Object array, StringBuilder xmlResult) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (array instanceof boolean[])
            {
                for (boolean anItem : (boolean[]) array)
                {
                    xmlResult.append("<boolean>");
                    xmlResult.append(anItem);
                    xmlResult.append("</boolean>");
                }
            }
            else if (array instanceof byte[])
            {
                encodeByteArray((byte[])array, xmlResult);
            }
            else if (array instanceof char[])
            {
                for (char anItem : (char[]) array)
                {
                    xmlResult.append("<char>");
                    xmlResult.append(anItem);
                    xmlResult.append("</char>");
                }
            }
            else if (array instanceof double[])
            {
                for (double anItem : (double[]) array)
                {
                    xmlResult.append("<double>");
                    xmlResult.append(anItem);
                    xmlResult.append("</double>");
                }
            }
            else if (array instanceof float[])
            {
                for (float anItem : (float[]) array)
                {
                    xmlResult.append("<float>");
                    xmlResult.append(anItem);
                    xmlResult.append("</float>");
                }
            }
            else if (array instanceof int[])
            {
                for (int anItem : (int[]) array)
                {
                    xmlResult.append("<int>");
                    xmlResult.append(anItem);
                    xmlResult.append("</int>");
                }
            }
            else if (array instanceof long[])
            {
                for (long anItem : (long[]) array)
                {
                    xmlResult.append("<long>");
                    xmlResult.append(anItem);
                    xmlResult.append("</long>");
                }
            }
            else if (array instanceof short[])
            {
                for (short anItem : (short[]) array)
                {
                    xmlResult.append("<short>");
                    xmlResult.append(anItem);
                    xmlResult.append("</short>");
                }
            }
            else if (array instanceof String[])
            {
                for (String anItem : (String[]) array)
                {
                    if (anItem != null)
                    {
                        xmlResult.append("<string>");
                        xmlResult.append(anItem);
                        xmlResult.append("</string>");
                    }
                    else
                    {
                        xmlResult.append("<string xsi:nil=\"true\"/>");
                    }
                }
            }
            // If it as an array declared as Object[]
            else if (array.getClass() == Object[].class)
            {
                for (Object anItem : (Object[]) array)
                {
                    String anAttributes = getAttributes(anItem, Object.class);
                    serializeElement("anyType", anAttributes, anItem, xmlResult);
                }
            }
            // If it is an array declared with some custom class. e.g. MyClass[].
            else if (array instanceof Object[])
            {
                for (Object anItem : (Object[]) array)
                {
                    String anElementName = getElementName(anItem.getClass());
                    serializeElement(anElementName, "", anItem, xmlResult);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private String getElementName(Class<?> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String anElementName;
            
            // Correct the name according to .NET
            if (clazz == Integer.class)
            {
                anElementName = "int";
            }
            else if (clazz == Character.class)
            {
                anElementName = "char";
            }
            else if (clazz == Byte.class)
            {
                anElementName = "unsignedByte";
            }
            else if (clazz == Boolean.class ||
                     clazz == Long.class || clazz == Short.class ||
                     clazz == Double.class || clazz == Float.class ||
                     clazz == String.class)
            {
                anElementName = clazz.getSimpleName().toLowerCase();
            }
            else if (clazz.isArray())
            {
                if (clazz == boolean[].class || clazz == Boolean[].class)
                {
                    anElementName = "ArrayOfBoolean";
                }
                else if (clazz == char[].class || clazz == Character[].class)
                {
                    anElementName = "ArrayOfChar";
                }
                else if (clazz == byte[].class || clazz == Byte[].class)
                {
                    anElementName = "base64Binary";
                }
                else if (clazz == int[].class || clazz == Integer[].class)
                {
                    anElementName = "ArrayOfInt";
                }
                else if (clazz == long[].class || clazz == Long[].class)
                {
                    anElementName = "ArrayOfLong";
                }
                else if (clazz == short[].class || clazz == Short[].class)
                {
                    anElementName = "ArrayOfShort";
                }
                else if (clazz == double[].class || clazz == Double[].class)
                {
                    anElementName = "ArrayOfDouble";
                }
                else if (clazz == float[].class || clazz == Float[].class)
                {
                    anElementName = "ArrayOfFloat";
                }
                else if (clazz == Object[].class)
                {
                    anElementName = "ArrayOfAnyType";
                }
                else
                {
                    String aClassItemTypeName;
                    Class<?> anItemType = clazz.getComponentType();
                    if (anItemType.isArray())
                    {
                        aClassItemTypeName = getElementName(anItemType);
                    }
                    else
                    {
                        aClassItemTypeName = anItemType.getSimpleName();
                    }

                    // Uppercase the first character.
                    if (aClassItemTypeName.length() > 1)
                    {
                        char aCapital = Character.toUpperCase(aClassItemTypeName.charAt(0));
                        aClassItemTypeName = aCapital + aClassItemTypeName.substring(1);
                    }
                    else
                    {
                        aClassItemTypeName.toUpperCase();
                    }
                    
                    anElementName = "ArrayOf" + aClassItemTypeName;
                }
            }
            else
            {
                anElementName = clazz.getSimpleName();
            }   
            
            return anElementName;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private String getAttributes(Object dataToSerialize, Class<?> declaredType)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String anAttributes = "";
            
            if (dataToSerialize == null)
            {
                // Put attributes indicating null.
                anAttributes = " xsi:nil=\"true\"";
            }
            else if (declaredType == Object.class)
            {
                // Put attributes indicating the real type.
                Class<?> aRealType = dataToSerialize.getClass();
                if (aRealType == boolean.class || aRealType == Boolean.class)
                {
                    anAttributes = " xsi:type=\"xsd:boolean\"";
                }
                else if (aRealType == char.class || aRealType == Character.class)
                {
                    anAttributes = " xmlns:q1=\"http://microsoft.com/wsdl/types/\" xsi:type=\"q1:char\"";
                }
                else if (aRealType == byte.class || aRealType == Byte.class)
                {
                    anAttributes = " xsi:type=\"xsd:unsignedByte\"";
                }
                else if (aRealType == int.class || aRealType == Integer.class)
                {
                    anAttributes = " xsi:type=\"xsd:int\"";
                }
                else if (aRealType == short.class || aRealType == Short.class)
                {
                    anAttributes = " xsi:type=\"xsd:short\"";
                }
                else if (aRealType == long.class || aRealType == Long.class)
                {
                    anAttributes = " xsi:type=\"xsd:long\"";
                }
                else if (aRealType == float.class || aRealType == Float.class)
                {
                    anAttributes = " xsi:type=\"xsd:fload\"";
                }
                else if (aRealType == double.class || aRealType == Double.class)
                {
                    anAttributes = " xsi:type=\"xsd:double\"";
                }
                else if (aRealType == String.class)
                {
                    anAttributes = " xsi:type=\"xsd:string\"";
                }
                else if (aRealType == byte[].class || aRealType == Byte[].class)
                {
                    anAttributes = " xsi:type=\"xsd:base64Binary\"";
                }
                else
                {
                    String anErrorMessage = "Serialized item of type 'Object' can be only primitive type or 'String' or byte[].";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }
            }
            
            return anAttributes;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    

    

    
    @SuppressWarnings("unchecked")
    private <T> T deserializePrimitiveType(XmlDataBrowser xmlBrowser, int elementStartPosition, int length, Class<T> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (clazz == boolean.class || clazz == Boolean.class)
            {
                Boolean aValue = xmlBrowser.getBooleanValue(elementStartPosition, length);
                return (T) aValue;
            }
            else if (clazz == byte.class || clazz == Byte.class)
            {
                Byte aValue = xmlBrowser.getByteValue(elementStartPosition, length);
                return (T) aValue;
            } 
            else if (clazz == char.class || clazz == Character.class)
            {
                // Get the number representing the character.
                int aNumber = xmlBrowser.getIntValue(elementStartPosition, length);
                
                // Convert the number to the character.
                Character aValue = (char)aNumber;
                
                return (T) aValue;
            }
            else if (clazz == int.class || clazz == Integer.class)
            {
                Integer aValue = xmlBrowser.getIntValue(elementStartPosition, length);
                return (T) aValue;
            }
            else if (clazz == long.class || clazz == Long.class)
            {
                Long aValue = xmlBrowser.getLongValue(elementStartPosition, length);
                return (T) aValue;
            }
            else if (clazz == short.class || clazz == Short.class)
            {
                Short aValue = xmlBrowser.getShortValue(elementStartPosition, length);
                return (T) aValue;
            }
            else if (clazz == float.class || clazz == Float.class)
            {
                Float aValue = xmlBrowser.getFloatValue(elementStartPosition, length);
                return (T) aValue;
            }
            else if (clazz == double.class || clazz == Double.class)
            {
                Double aValue = xmlBrowser.getDoubleValue(elementStartPosition, length);
                return (T) aValue;
            }
            
            return null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T deserializeArray(XmlDataBrowser xmlBrowser, int elementStartPosition, int length, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: everybody needs this except byte array.
            ArrayList<XmlDataBrowser.TElement> anElements = (clazz != byte[].class && clazz != Byte[].class)
                    ? xmlBrowser.getElements(elementStartPosition, length) : null;
            
            if (clazz == boolean[].class || clazz == Boolean[].class)
            {
                boolean[] anItems = new boolean[anElements.size()];
                for (int i = 0; i < anItems.length; ++i)
                {
                    XmlDataBrowser.TElement anElement = anElements.get(i);
                    boolean aValue = xmlBrowser.getBooleanValue(anElement.myValueStartPosition, anElement.myValueLength);
                    anItems[i] = aValue;
                }
    
                return (T) anItems;
            }
            else if (clazz == byte[].class || clazz == Byte[].class)
            {
                byte[] anItems = decodeByteArray(xmlBrowser, elementStartPosition, length);
    
                return (T) anItems;
            }
            else if (clazz == char[].class || clazz == Character[].class)
            {
                char[] anItems = new char[anElements.size()];
                for (int i = 0; i < anItems.length; ++i)
                {
                    XmlDataBrowser.TElement anElement = anElements.get(i);
                    
                    // Get number representing the char.
                    int aNumber = xmlBrowser.getIntValue(anElement.myValueStartPosition, anElement.myValueLength);
                    anItems[i] = (char) aNumber;
                }
    
                return (T) anItems;
            }
            else if (clazz == int[].class || clazz == Integer[].class)
            {
                int[] anItems = new int[anElements.size()];
                for (int i = 0; i < anItems.length; ++i)
                {
                    XmlDataBrowser.TElement anElement = anElements.get(i);
                    int aValue = xmlBrowser.getIntValue(anElement.myValueStartPosition, anElement.myValueLength);
                    anItems[i] = aValue;
                }
    
                return (T) anItems;
            }
            else if (clazz == long[].class || clazz == Long[].class)
            {
                long[] anItems = new long[anElements.size()];
                for (int i = 0; i < anItems.length; ++i)
                {
                    XmlDataBrowser.TElement anElement = anElements.get(i);
                    long aValue = xmlBrowser.getLongValue(anElement.myValueStartPosition, anElement.myValueLength);
                    anItems[i] = aValue;
                }
    
                return (T) anItems;
            }
            else if (clazz == short[].class || clazz == Short[].class)
            {
                short[] anItems = new short[anElements.size()];
                for (int i = 0; i < anItems.length; ++i)
                {
                    XmlDataBrowser.TElement anElement = anElements.get(i);
                    short aValue = xmlBrowser.getShortValue(anElement.myValueStartPosition, anElement.myValueLength);
                    anItems[i] = aValue;
                }
    
                return (T) anItems;
            }
            else if (clazz == double[].class || clazz == Double[].class)
            {
                double[] anItems = new double[anElements.size()];
                for (int i = 0; i < anItems.length; ++i)
                {
                    XmlDataBrowser.TElement anElement = anElements.get(i);
                    double aValue = xmlBrowser.getDoubleValue(anElement.myValueStartPosition, anElement.myValueLength);
                    anItems[i] = aValue;
                }
    
                return (T) anItems;
            }
            else if (clazz == float[].class || clazz == Float[].class)
            {
                float[] anItems = new float[anElements.size()];
                for (int i = 0; i < anItems.length; ++i)
                {
                    XmlDataBrowser.TElement anElement = anElements.get(i);
                    float aValue = xmlBrowser.getFloatValue(anElement.myValueStartPosition, anElement.myValueLength);
                    anItems[i] = aValue;
                }
    
                return (T) anItems;
            }
            else if (clazz == String[].class)
            {
                String[] anItems = new String[anElements.size()];
                for (int i = 0; i < anItems.length; ++i)
                {
                    XmlDataBrowser.TElement anElement = anElements.get(i);
                    if (!anElement.myIsNull)
                    {
                        String aValue = xmlBrowser.getStringValue(anElement.myValueStartPosition, anElement.myValueLength);
                        anItems[i] = aValue;
                    }
                    else
                    {
                        anItems[i] = null;
                    }
                }
    
                return (T) anItems;
            }
            
            // It was not an array with a primitive type.
            // So it must be an array with some enum or some class.
            Class<?> anArrayItemType = clazz.getComponentType();
            T[] anItems = (T[]) Array.newInstance(anArrayItemType, anElements.size());
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlDataBrowser.TElement anElement = anElements.get(i);
                T aValue = (T) deserializeElement(xmlBrowser, anElement, anArrayItemType);
                anItems[i] = aValue;
            }
            
            return (T) anItems;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    // Encodes a byte array into Base64 format. 
    private void encodeByteArray(byte[] bytes, StringBuilder xmlResult)
    {
        int oDataLen = (bytes.length * 4 + 2) / 3; // output length without padding
       
        int ip = 0;
        int iEnd = bytes.length ;
        int op = 0;
        while (ip < iEnd)
        {
            int i0 = bytes[ip++] & 0xff;
            int i1 = ip < iEnd ? bytes[ip++] & 0xff : 0;
            int i2 = ip < iEnd ? bytes[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            
            xmlResult.append(map1[o0]);
            ++op;
            
            xmlResult.append(map1[o1]);
            ++op;
            
            xmlResult.append(op < oDataLen ? map1[o2] : '=');
            ++op;
            
            xmlResult.append(op < oDataLen ? map1[o3] : '=');
            ++op;
        }
    }
    
    // Decodes a byte array from Base64 format. 
    private byte[] decodeByteArray(XmlDataBrowser xmlBrowser, int elementStartPosition, int length)
    {
        int iLen = length;
        
        if (iLen % 4 != 0)
        {
            throw new IllegalArgumentException ("Length of Base64 encoded input string is not a multiple of 4.");
        }
        
        while (iLen > 0 && xmlBrowser.getCharValue(elementStartPosition + iLen-1) == '=')
        {
            iLen--;
        }
        
        int oLen = (iLen * 3) / 4;
        byte[] out = new byte[oLen];
        
        int ip = elementStartPosition;
        int iEnd = elementStartPosition + iLen;
        int op = 0;
        while (ip < iEnd)
        {
            int i0 = xmlBrowser.getCharValue(ip++);
            int i1 = xmlBrowser.getCharValue(ip++);
            int i2 = ip < iEnd ? xmlBrowser.getCharValue(ip++) : 'A';
            int i3 = ip < iEnd ? xmlBrowser.getCharValue(ip++) : 'A';
            
            if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127)
            {
                throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
            }
            
            int b0 = map2[i0];
            int b1 = map2[i1];
            int b2 = map2[i2];
            int b3 = map2[i3];
            
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
            {
                throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
            }
            
            int o0 = ( b0 <<2) | (b1>>>4);
            int o1 = ((b1 & 0xf)<<4) | (b2>>>2);
            int o2 = ((b2 & 3)<<6) | b3;
            out[op++] = (byte)o0;
            
            if (op<oLen)
            {
                out[op++] = (byte)o1;
            }
            
            if (op<oLen)
            {
                out[op++] = (byte)o2;
            }
        }
        
        return out;
    }
    
    
    // Mapping table from 6-bit nibbles to Base64 characters.
    private final char[] map1 = new char[64];
  
    // Mapping table from Base64 characters to 6-bit nibbles.
    private final byte[] map2 = new byte[128];
}

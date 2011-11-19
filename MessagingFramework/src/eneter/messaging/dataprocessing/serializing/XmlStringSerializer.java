/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

import java.lang.reflect.*;
import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements the serialization/deserialization to/from XmlString. The
 * serializer can be used for the communication with Eneter.Messaging.Framework
 * for .NET. The serializer does not support generic types.
 * 
 * @author Ondrej Uzovic & Martin Valach
 * 
 * TODO: Encoding/Decoding UTF-8. (.NET works with UTF-8, Java works with UTF-16).
 */
public class XmlStringSerializer implements ISerializer
{
    private class XmlDataBrowser
    {
        public class TElement
        {
            public String myName = "";
            public int myValueStartPosition;
            public int myValueLength;
            public int myNextElementStartPosition;
            public boolean myIsNull;
        }

        public XmlDataBrowser(String xmlString) 
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myXmlString = xmlString;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public ArrayList<TElement> getElements(int startIdx, int length)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                ArrayList<TElement> anElements = new ArrayList<TElement>();
    
                int anIdx = startIdx;
                while (anIdx < startIdx + length)
                {
                    TElement anElement = getElement(anIdx);
                    anElements.add(anElement);
    
                    // Set the position to the beginning of the next element.
                    anIdx = anElement.myNextElementStartPosition;
                }
    
                return anElements;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public TElement getElement(int startIdx)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                // Get the starting of the element. '<'
                int anIdx = getNonwhitePosition(startIdx);
                if (anIdx == -1)
                {
                    String anErrorMsg = "Incorrect xml format.";
                    EneterTrace.error(anErrorMsg);
                    throw new IllegalStateException(anErrorMsg);
                }
                if (myXmlString.charAt(anIdx) != '<')
                {
                    String anErrorMsg = "The xml string does not start with '<' character.";
                    EneterTrace.error(anErrorMsg);
                    throw new IllegalStateException(anErrorMsg);
                }
    
                TElement anElement = new TElement();
    
                // Go through the string to get the element name.
                ++anIdx;
                char c = getChar(anIdx);
                if (c == Character.MIN_VALUE || isWhiteCharacter(c))
                {
                    String anErrorMsg = "Unexpected space character after '<'.";
                    EneterTrace.error(anErrorMsg);
                    throw new IllegalStateException(anErrorMsg);
                }
                while (c != '>' && !isWhiteCharacter(c))
                {
                    anElement.myName += c;
    
                    ++anIdx;
                    c = myXmlString.charAt(anIdx);
                }
    
                // If we did not find '>'. Then there was a space and attributes can follow.
                // Check if there is a nil attribute set to true. -> it means the value is null.
                int anAttributeCheckIdx = 0;
                while (c != '>')
                {
                    ++anIdx;
                    c = getChar(anIdx);
                    if (c == Character.MIN_VALUE)
                    {
                        String anErrorMsg = "'>' is missing for '" + anElement.myName + "'.";
                        EneterTrace.error(anErrorMsg);
                        throw new IllegalStateException(anErrorMsg);
                    }
                    
                    // Check if there is a nil attribute.
                    if (anElement.myIsNull == false && c == NILLATTRIBUTE.charAt(anAttributeCheckIdx))
                    {
                        ++anAttributeCheckIdx;
                        if (anAttributeCheckIdx == NILLATTRIBUTE.length())
                        {
                            anElement.myIsNull = true;
                        }
                    }
                }

                // If the element is null.
                if (anElement.myIsNull)
                {
                    anElement.myNextElementStartPosition = anIdx + 1;
                    anElement.myValueStartPosition = anIdx + 1;
                    anElement.myValueLength = 0;
                    
                    return anElement;
                }
                
                // Store the position to first character behind '>'
                anElement.myValueStartPosition = anIdx + 1;
    
                String aBeginningElement = "<" + anElement.myName;
                String anEndingElement = "</" + anElement.myName + ">";
    
                // Get the length of the element value
                int anEqualToBeginningIdx = 0;
                int anEqualToEndingIdx = 0;
                int aRecursiveDepth = 1;
                while (aRecursiveDepth > 0)
                {
                    // Go to the next character.
                    ++anIdx;
    
                    // Read the character.
                    c = getChar(anIdx);
                    if (c == Character.MIN_VALUE)
                    {
                        String anErrorMsg = "The reading of the xml string failed.";
                        EneterTrace.error(anErrorMsg);
                        throw new IllegalStateException(anErrorMsg);
                    }
    
                    // If there is a beginning of the sub element with the same name
                    // => recursive element.
                    if (anEqualToBeginningIdx == aBeginningElement.length())
                    {
                        if (c == '>' || isWhiteCharacter(c))
                        {
                            // We have the beginning of "recursive" element with the
                            // same name.
                            ++aRecursiveDepth;
                        }
    
                        // Detection of another recursive element can start.
                        anEqualToBeginningIdx = 0;
                    }
                    // If the character matches with the character indicating the
                    // possibility
                    // that it can be a beginning of the recursive element.
                    else if (c == aBeginningElement.charAt(anEqualToBeginningIdx))
                    {
                        // Indicate that character matched.
                        ++anEqualToBeginningIdx;
                    }
                    else
                    {
                        // Character did not match, so start detecting of the
                        // recursive
                        // element from the beginning.
                        anEqualToBeginningIdx = 0;
                    }
    
                    // If the character matches with the character indicating the
                    // possibility
                    // that it can be the ending of the element.
                    if (c == anEndingElement.charAt(anEqualToEndingIdx))
                    {
                        // Indicate that the character matched.
                        ++anEqualToEndingIdx;
    
                        // If all character matched, then it was the ending element.
                        if (anEqualToEndingIdx == anEndingElement.length())
                        {
                            // If there was recursive element, then this ending
                            // element
                            // ended this recursive element.
                            --aRecursiveDepth;
    
                            // Start detecting the ending element again.
                            anEqualToEndingIdx = 0;
                        }
                    }
                    // If id is not the ending element.
                    else
                    {
                        // If there was some detection of the ending element but
                        // finally
                        // it was not the ending element, then include the length of
                        // that
                        // substring too.
                        if (anEqualToEndingIdx > 0)
                        {
                            // Increase the length of the value part about the
                            // unsuccessful detection.
                            anElement.myValueLength += anEqualToEndingIdx;
    
                            // Start detecting the ending element again.
                            anEqualToEndingIdx = 0;
                        }
    
                        // Increase the length of the value part about the
                        // character.
                        ++anElement.myValueLength;
                    }
                }
    
                // Store the beginning of the next element.
                anElement.myNextElementStartPosition = anIdx + 1;
    
                return anElement;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public String getStringValue(int startIdx, int length)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                String aResult = "";
    
                // Get the substring from the given position and decode xml key
                // words.
                // Note: The loop must do it on one pass and without copying
                // strings.
                // Creating copies and new objects in memory would significantly
                // degraded the performance.
                for (int i = startIdx; i < startIdx + length; ++i)
                {
                    char c = myXmlString.charAt(i);
    
                    // If it is not an xml keyword, then just add the char to the
                    // string.
                    if (c != '&')
                    {
                        aResult += c;
                    } else
                    {
                        ++i;
                        c = getChar(i);
                        if (c != Character.MIN_VALUE)
                        {
    
                            // detecting &amp; or &apos;
                            if (c == 'a')
                            {
                                ++i;
                                c = getChar(i);
                                if (c != Character.MIN_VALUE)
                                {
                                    // detecting &amp;
                                    if (c == 'm')
                                    {
                                        ++i;
                                        c = getChar(i);
                                        if (c != Character.MIN_VALUE)
                                        {
                                            if (c == 'p')
                                            {
                                                ++i;
                                                c = getChar(i);
                                                if (c != Character.MIN_VALUE)
                                                {
                                                    if (c == ';')
                                                    {
                                                        aResult += '&';
                                                    } else
                                                    {
                                                        aResult += "&amp";
                                                        aResult += c;
                                                    }
                                                } else
                                                {
                                                    aResult += "&amp";
                                                }
                                            } else
                                            {
                                                aResult += "&am";
                                                aResult += c;
                                            }
                                        } else
                                        {
                                            aResult += "&am";
                                        }
                                    } else if (c == 'p')
                                    {
                                        ++i;
                                        c = getChar(i);
                                        if (c != Character.MIN_VALUE)
                                        {
                                            if (c == 'o')
                                            {
                                                ++i;
                                                c = getChar(i);
                                                if (c != Character.MIN_VALUE)
                                                {
                                                    if (c == 's')
                                                    {
                                                        ++i;
                                                        c = getChar(i);
                                                        if (c != Character.MIN_VALUE)
                                                        {
                                                            if (c == ';')
                                                            {
                                                                aResult += '\'';
                                                            } else
                                                            {
                                                                aResult += "&apos";
                                                                aResult += c;
                                                            }
                                                        } else
                                                        {
                                                            aResult += "&apos";
                                                        }
                                                    } else
                                                    {
                                                        aResult += "&apo";
                                                        aResult += c;
                                                    }
                                                } else
                                                {
                                                    aResult += "&apo";
                                                }
                                            } else
                                            {
                                                aResult += "&ap";
                                                aResult += c;
                                            }
                                        } else
                                        {
                                            aResult += "&ap";
                                        }
                                    } else
                                    {
                                        aResult += "&a";
                                        aResult += c;
                                    }
                                } else
                                {
                                    aResult += "&a";
                                }
                            }
    
                            // detecting &lt;
                            else if (c == 'l')
                            {
                                ++i;
                                c = getChar(i);
                                if (c != Character.MIN_VALUE)
                                {
                                    if (c == 't')
                                    {
                                        ++i;
                                        c = getChar(i);
                                        if (c != Character.MIN_VALUE)
                                        {
                                            if (c == ';')
                                            {
                                                aResult += '<';
                                            } else
                                            {
                                                aResult += "&lt";
                                                aResult += c;
                                            }
                                        } else
                                        {
                                            aResult += "&lt";
                                        }
                                    } else
                                    {
                                        aResult += "&l";
                                        aResult += c;
                                    }
                                } else
                                {
                                    aResult += "&l";
                                }
                            }
    
                            // detecting &gt;
                            else if (c == 'g')
                            {
                                ++i;
                                c = getChar(i);
                                if (c != Character.MIN_VALUE)
                                {
                                    if (c == 't')
                                    {
                                        ++i;
                                        c = getChar(i);
                                        if (c != Character.MIN_VALUE)
                                        {
                                            if (c == ';')
                                            {
                                                aResult += '>';
                                            } else
                                            {
                                                aResult += "&gt";
                                                aResult += c;
                                            }
                                        } else
                                        {
                                            aResult += "&gt";
                                        }
                                    } else
                                    {
                                        aResult += "&g";
                                        aResult += c;
                                    }
                                } else
                                {
                                    aResult += "&g";
                                }
                            }
    
                            // detecting &quot;
                            else if (c == 'q')
                            {
                                ++i;
                                c = getChar(i);
                                if (c != Character.MIN_VALUE)
                                {
                                    if (c == 'u')
                                    {
                                        ++i;
                                        c = getChar(i);
                                        if (c != Character.MIN_VALUE)
                                        {
                                            if (c == 'o')
                                            {
                                                ++i;
                                                c = getChar(i);
                                                if (c != Character.MIN_VALUE)
                                                {
                                                    if (c == 't')
                                                    {
                                                        ++i;
                                                        c = getChar(i);
                                                        if (c != Character.MIN_VALUE)
                                                        {
                                                            if (c == ';')
                                                            {
                                                                aResult += '"';
                                                            } else
                                                            {
                                                                aResult += "&quot";
                                                                aResult += c;
                                                            }
                                                        } else
                                                        {
                                                            aResult += "&quot";
                                                        }
                                                    } else
                                                    {
                                                        aResult += "&quo";
                                                        aResult += c;
                                                    }
                                                } else
                                                {
                                                    aResult += "&quo";
                                                }
                                            } else
                                            {
                                                aResult += "&qu";
                                                aResult += c;
                                            }
                                        } else
                                        {
                                            aResult += "&qu";
                                        }
                                    } else
                                    {
                                        aResult += "&q";
                                        aResult += c;
                                    }
                                } else
                                {
                                    aResult += "&q";
                                }
                            } else
                            {
                                aResult += '&';
                                aResult += c;
                            }
                        } else
                        {
                            aResult += '&';
                        }
                    }
                }
    
                return aResult;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public boolean getBooleanValue(int startIdx, int length)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                String aValueStr = myXmlString.substring(startIdx, startIdx + length);
                return Boolean.parseBoolean(aValueStr);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public byte getByteValue(int startIdx, int length)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                String aValueStr = myXmlString.substring(startIdx, startIdx + length);
                return Byte.parseByte(aValueStr);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public int getIntValue(int startIdx, int length)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                String aValueStr = myXmlString.substring(startIdx, startIdx + length);
                return Integer.parseInt(aValueStr);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public long getLongValue(int startIdx, int length)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                String aValueStr = myXmlString.substring(startIdx, startIdx + length);
                return Long.parseLong(aValueStr);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public short getShortValue(int startIdx, int length)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                String aValueStr = myXmlString.substring(startIdx, startIdx + length);
                return Short.parseShort(aValueStr);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public double getDoubleValue(int startIdx, int length)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                String aValueStr = myXmlString.substring(startIdx, startIdx + length);
                return Double.parseDouble(aValueStr);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public float getFloatValue(int startIdx, int length)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                String aValueStr = myXmlString.substring(startIdx, startIdx + length);
                return Float.parseFloat(aValueStr);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        private int getNonwhitePosition(int startIdx)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                for (int i = startIdx; i < myXmlString.length(); ++i)
                {
                    char c = myXmlString.charAt(i);
    
                    if (c != ' ' && c != '\t' && c != '\n' && c != '\f' && c != '\r')
                    {
                        return i;
                    }
                }
                
                return -1;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        // Note: Do not put trace into this method.
        private char getChar(int idx)
        {
            if (idx < myXmlString.length())
            {
                char c = myXmlString.charAt(idx);
                return c;
            }

            return Character.MIN_VALUE;
        }

        // Note: Do not put trace into this method.
        private boolean isWhiteCharacter(char c)
        {
            return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
        }

        private String myXmlString;
        
        private final String NILLATTRIBUTE = "xsi:nil=\"true\"";
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

            String aRootName = clazz.getSimpleName();
            
            // Correct the name according to .NET
            if (clazz.isArray())
            {
                if (clazz == boolean[].class || clazz == Boolean[].class)
                {
                    aRootName = "ArrayOfBoolean";
                }
                else if (clazz == char[].class || clazz == Character[].class)
                {
                    aRootName = "ArrayOfChar";
                }
                else if (clazz == byte[].class || clazz == Byte[].class)
                {
                    aRootName = "base64Binary";
                }
                else if (clazz == int[].class || clazz == Integer[].class)
                {
                    aRootName = "ArrayOfInt";
                }
                else if (clazz == long[].class || clazz == Long[].class)
                {
                    aRootName = "ArrayOfLong";
                }
                else if (clazz == short[].class || clazz == Short[].class)
                {
                    aRootName = "ArrayOfShort";
                }
                else if (clazz == double[].class || clazz == Double[].class)
                {
                    aRootName = "ArrayOfDouble";
                }
                else if (clazz == float[].class || clazz == Float[].class)
                {
                    aRootName = "ArrayOfFloat";
                }
                else
                {
                    String aClassItemTypeName = clazz.getComponentType().getSimpleName();
                    aRootName = "ArrayOf" + aClassItemTypeName;
                }
            }
            
            // Note: keep the space character at the beginning of the name-space string!!!
            serializeElement(aRootName, " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", dataToSerialize, aSerializedObjectStr);

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
            // If it is null then put there the null attribute.
            if (dataToSerialize == null)
            {
                xmlResult.append("<");
                xmlResult.append(xmlElementName);
                xmlResult.append(attributeSection);
                xmlResult.append(" xsi:nil=\"true\"/>");
                
                return;
            }
            
            xmlResult.append("<");
            xmlResult.append(xmlElementName);
            xmlResult.append(attributeSection);
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
                    // If the object has some members then serialize them.
                    Field[] aFields = aSerializedType.getFields();
                    for (Field aField : aFields)
                    {
                        serializeElement(aField.getName(), "", aField.get(dataToSerialize), xmlResult);
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
            // Ok, then it is some class
            else
            {
                // Get value of the element.
                ArrayList<XmlDataBrowser.TElement> anElements = xmlBrowser.getElements(xmlElement.myValueStartPosition, xmlElement.myValueLength);
    
                // Create the instance of deserialized object via default constructor.
                T aDeserializedObject = (T) clazz.newInstance();
    
                // Get public fields of the deserialized object.
                Field[] aFields = clazz.getFields();
    
                // Go through fields and deserialize them.
                int aSearchIdx = 0;
                for (Field aField : aFields)
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
    
                    // If the element was not found, then there is an error.
                    if (anElement == null)
                    {
                        String anErrorMsg = "Field '" + aFieldName + "' is missing.";
                        EneterTrace.error(anErrorMsg);
                        throw new IllegalStateException(anErrorMsg);
                    }
                    
                    // Recursively deserialize the object for the field.
                    Object aValue = deserializeElement(xmlBrowser, anElement, aField.getType());
    
                    // Set the created object to the field.
                    aDeserializedObject.getClass().getField(aFieldName).set(aDeserializedObject, aValue);
                }
    
                return aDeserializedObject;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    /**
     * Parses the given string and replaces the xml specific characters.
     * @param s
     * @return
     */
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
            } else if (array instanceof byte[])
            {
             // TODO: investigate how .NET puts byte[] values to xml.
                //for (byte anItem : (byte[]) array)
                //{
                    //xmlResult.append("<byte>");
                    //xmlResult.append(anItem);
                    //xmlResult.append("</byte>");
                //}
            } else if (array instanceof char[])
            {
                for (char anItem : (char[]) array)
                {
                    xmlResult.append("<char>");
                    xmlResult.append(anItem);
                    xmlResult.append("</char>");
                }
            } else if (array instanceof double[])
            {
                for (double anItem : (double[]) array)
                {
                    xmlResult.append("<double>");
                    xmlResult.append(anItem);
                    xmlResult.append("</double>");
                }
            } else if (array instanceof float[])
            {
                for (float anItem : (float[]) array)
                {
                    xmlResult.append("<float>");
                    xmlResult.append(anItem);
                    xmlResult.append("</float>");
                }
            } else if (array instanceof int[])
            {
                for (int anItem : (int[]) array)
                {
                    xmlResult.append("<int>");
                    xmlResult.append(anItem);
                    xmlResult.append("</int>");
                }
            } else if (array instanceof long[])
            {
                for (long anItem : (long[]) array)
                {
                    xmlResult.append("<long>");
                    xmlResult.append(anItem);
                    xmlResult.append("</long>");
                }
            } else if (array instanceof short[])
            {
                for (short anItem : (short[]) array)
                {
                    xmlResult.append("<short>");
                    xmlResult.append(anItem);
                    xmlResult.append("</short>");
                }
            } else if (array instanceof Object[])
            {
                for (Object anItem : (Object[]) array)
                {
                    serializeElement(anItem.getClass().getSimpleName(), "", anItem, xmlResult);
                }
            }
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
            ArrayList<XmlDataBrowser.TElement> anElements = xmlBrowser.getElements(elementStartPosition, length);
            
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
                byte[] anItems = new byte[anElements.size()];
                for (int i = 0; i < anItems.length; ++i)
                {
                 // TODO: investigate how .NET puts byte[] values to xml.
                    //XmlDataBrowser.TElement anElement = anElements.get(i);
                    //byte aValue = xmlBrowser.getByteValue(anElement.myValueStartPosition, anElement.myValueLength);
                    //anItems[i] = aValue;
                }
    
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
                    String aValue = xmlBrowser.getStringValue(anElement.myValueStartPosition, anElement.myValueLength);
                    anItems[i] = aValue;
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

}

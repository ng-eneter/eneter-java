package eneter.messaging.dataprocessing.serializing;

import java.lang.reflect.*;
import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.Iterator;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.IFunction1;

public class XmlStringSerializer implements ISerializer
{
    private class XmlBrowser
    {
        public class TElement
        {
            public String myName = "";
            public int myValueStartPosition;
            public int myValueLength;
            public int myNextElementStartPosition;
        }
        
        public XmlBrowser(String xmlString)
        {
            myXmlString = xmlString;
        }
        
        public ArrayList<TElement> getElements(int startIdx, int length)
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
        
        public TElement getElement(int startIdx)
        {
            // Get the starting of the element. '<'
            int anIdx = getNonwhitePosition(startIdx);
            if (anIdx == -1)
            {
                throw new IllegalStateException("The provided string is not xml.");
            }
            if (myXmlString.charAt(anIdx) != '<')
            {
                throw new IllegalStateException("The xml string does not start with '<' character.");
            }
            
            TElement anElement = new TElement();
            
            // Go through the string to get the element name.
            ++anIdx;
            char c = getChar(anIdx);
            if (c == Character.MIN_VALUE || isWhiteCharacter(c))
            {
                throw new IllegalStateException("Unexpected space character after '<'.");
            }
            while (c != '>' && !isWhiteCharacter(c))
            {
                anElement.myName += c;
                
                ++anIdx;
                c = myXmlString.charAt(anIdx);
            }
            
            // If we did not find '>'. Then there was a space and attributes can follow.
            // We must just find the end.
            while (c != '>')
            {
                ++anIdx;
                c = getChar(anIdx);
                if (c == Character.MIN_VALUE)
                {
                    throw new IllegalStateException("'>' is missing for '" + anElement.myName + "'." );
                }
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
                    throw new IllegalStateException("The reading of the xml string failed.");
                }

                // If there is a beginning of the sub element with the same name => recursive element.
                if (anEqualToBeginningIdx == aBeginningElement.length())
                {
                    if (c == '>' || isWhiteCharacter(c))
                    {
                        // We have the beginning of "recursive" element with the same name.
                        ++aRecursiveDepth;
                    }

                    // Detection of another recursive element can start.
                    anEqualToBeginningIdx = 0;
                }
                // If the character matches with the character indicating the possibility
                // that it can be a beginning of the recursive element.
                else if (c == aBeginningElement.charAt(anEqualToBeginningIdx))
                {
                    // Indicate that character matched.
                    ++anEqualToBeginningIdx;
                }
                else
                {
                    // Character did not match, so start detecting of the recursive
                    // element from the beginning.
                    anEqualToBeginningIdx = 0;
                }

                // If the character matches with the character indicating the possibility
                // that it can be the ending of the element.
                if (c == anEndingElement.charAt(anEqualToEndingIdx))
                {
                    // Indicate that the character matched.
                    ++anEqualToEndingIdx;

                    // If all character matched, then it was the ending element.
                    if (anEqualToEndingIdx == anEndingElement.length())
                    {
                        // If there was recursive element, then this ending element
                        // ended this recursive element.
                        --aRecursiveDepth;
                        
                        // Start detecting the ending element again.
                        anEqualToEndingIdx = 0;
                    }
                }
                // If id is not the ending element.
                else
                {
                    // If there was some detection of the ending element but finally
                    // it was not the ending element, then include the length of that
                    // substring too.
                    if (anEqualToEndingIdx > 0)
                    {
                        // Increase the length of the value part about the unsuccessful detection.
                        anElement.myValueLength += anEqualToEndingIdx;
                        
                        // Start detecting the ending element again.
                        anEqualToEndingIdx = 0;
                    }

                    // Increase the length of the value part about the character.
                    ++anElement.myValueLength;
                }
            }
            
            // Store the beginning of the next element.
            anElement.myNextElementStartPosition = anIdx + 1;
            
            return anElement;
        }
        
        public String getStringValue(int startIdx, int length)
        {
            return myXmlString.substring(startIdx, startIdx + length);
        }
        
        public boolean getBooleanValue(int startIdx, int length)
        {
            String aValueStr = myXmlString.substring(startIdx, startIdx + length);
            return Boolean.parseBoolean(aValueStr);
        }
        
        public byte getByteValue(int startIdx, int length)
        {
            String aValueStr = myXmlString.substring(startIdx, startIdx + length);
            return Byte.parseByte(aValueStr);
        }
        
        public char getCharValue(int startIdx)
        {
            return myXmlString.charAt(startIdx);
        }
        
        public int getIntValue(int startIdx, int length)
        {
            String aValueStr = myXmlString.substring(startIdx, startIdx + length);
            return Integer.parseInt(aValueStr);
        }
        
        public long getLongValue(int startIdx, int length)
        {
            String aValueStr = myXmlString.substring(startIdx, startIdx + length);
            return Long.parseLong(aValueStr);
        }
        
        public short getShortValue(int startIdx, int length)
        {
            String aValueStr = myXmlString.substring(startIdx, startIdx + length);
            return Short.parseShort(aValueStr);
        }
        
        public double getDoubleValue(int startIdx, int length)
        {
            String aValueStr = myXmlString.substring(startIdx, startIdx + length);
            return Double.parseDouble(aValueStr);
        }
        
        public float getFloatValue(int startIdx, int length)
        {
            String aValueStr = myXmlString.substring(startIdx, startIdx + length);
            return Float.parseFloat(aValueStr);
        }
        
        private int getNonwhitePosition(int startIdx)
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
        
        private char getChar(int idx)
        {
            if (idx < myXmlString.length())
            {
                char c = myXmlString.charAt(idx);
                return c;
            }
            
            return Character.MIN_VALUE;
        }
        
        private boolean isWhiteCharacter(char c)
        {
            return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
        }
        
        private String myXmlString;
    }
    
    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: The buffer size in StringBuilder is 16 bytes by default.
            //       Serialized structures will need more.
            //       So to avoid allocations initialize it with 500.
            StringBuilder aSerializedObjectStr = new StringBuilder(500);

            String aRootName = clazz.getSimpleName();
            buildXml(aRootName, dataToSerialize, aSerializedObjectStr);
            
            return aSerializedObjectStr.toString();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <T> T deserialize(Object serializedData, Class<T> clazz) throws Exception
    {
        if (serializedData == null)
        {
            throw new IllegalArgumentException("Input parameter 'serializedData' is null.");
        }
        
        if (serializedData instanceof String == false)
        {
            throw new IllegalArgumentException("Input parameter 'serializedData' is not String.");
        }
        
        // Create browser to read the xml from the string.
        XmlBrowser anXmlBrowser = new XmlBrowser((String)serializedData);

        // Get the root element.
        XmlBrowser.TElement aRootElement = anXmlBrowser.getElement(0);

        T aDeserializedObject = deserializeElement(anXmlBrowser, aRootElement.myValueStartPosition, aRootElement.myValueLength, clazz);
        
        return aDeserializedObject;
    }
    
    private <T> T deserializeElement(XmlBrowser anXmlBrowser, int elementStartPosition, int length, Class<T> clazz)
        throws Exception
    {
        String aDeserializedTypeName = clazz.getSimpleName();
        
        // If it is a simple type (not a custom class) then the field contains the value.
        if (aDeserializedTypeName.equals("boolean") || aDeserializedTypeName.equals("Boolean"))
        {
            Boolean aValue = anXmlBrowser.getBooleanValue(elementStartPosition, length);
            return (T)aValue;
        }
        else if (aDeserializedTypeName.equals("byte") || aDeserializedTypeName.equals("Byte"))
        {
            Byte aValue = anXmlBrowser.getByteValue(elementStartPosition, length);
            return (T)aValue;
        }
        else if (aDeserializedTypeName.equals("char") || aDeserializedTypeName.equals("Character"))
        {
            Character aValue = anXmlBrowser.getCharValue(elementStartPosition);
            return (T)aValue;
        }
        else if (aDeserializedTypeName.equals("int") || aDeserializedTypeName.equals("Integer"))
        {
            Integer aValue = anXmlBrowser.getIntValue(elementStartPosition, length);
            return (T)aValue;
        }
        else if (aDeserializedTypeName.equals("long") || aDeserializedTypeName.equals("Long"))
        {
            Long aValue = anXmlBrowser.getLongValue(elementStartPosition, length);
            return (T)aValue;
        }
        else if (aDeserializedTypeName.equals("short") || aDeserializedTypeName.equals("Short"))
        {
            Short aValue = anXmlBrowser.getShortValue(elementStartPosition, length);
            return (T)aValue;
        }
        else if (aDeserializedTypeName.equals("float") || aDeserializedTypeName.equals("Float"))
        {
            Float aValue = anXmlBrowser.getFloatValue(elementStartPosition, length);
            return (T)aValue;
        }
        else if (aDeserializedTypeName.equals("double") || aDeserializedTypeName.equals("Double"))
        {
            Double aValue = anXmlBrowser.getDoubleValue(elementStartPosition, length);
            return (T)aValue;
        }
        else if (aDeserializedTypeName.equals("String"))
        {
            String aValue = anXmlBrowser.getStringValue(elementStartPosition, length);
            return (T)aValue;
        }
        // If it is an array
        else if (aDeserializedTypeName.equals("boolean[]") || aDeserializedTypeName.equals("Boolean[]"))
        {
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            boolean[] anItems = new boolean[anElements.size()];
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlBrowser.TElement anElement = anElements.get(i);
                boolean aValue = anXmlBrowser.getBooleanValue(anElement.myValueStartPosition, anElement.myValueLength);
                anItems[i] = aValue;
            }
            
            return (T)anItems;
        }
        else if (aDeserializedTypeName.equals("byte[]") || aDeserializedTypeName.equals("Byte[]"))
        {
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            byte[] anItems = new byte[anElements.size()];
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlBrowser.TElement anElement = anElements.get(i);
                byte aValue = anXmlBrowser.getByteValue(anElement.myValueStartPosition, anElement.myValueLength);
                anItems[i] = aValue;
            }
            
            return (T)anItems;
        }
        else if (aDeserializedTypeName.equals("char[]") || aDeserializedTypeName.equals("Character[]"))
        {
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            char[] anItems = new char[anElements.size()];
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlBrowser.TElement anElement = anElements.get(i);
                char aValue = anXmlBrowser.getCharValue(anElement.myValueStartPosition);
                anItems[i] = aValue;
            }
            
            return (T)anItems;
        }
        else if (aDeserializedTypeName.equals("int[]") || aDeserializedTypeName.equals("Integer[]"))
        {
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            int[] anItems = new int[anElements.size()];
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlBrowser.TElement anElement = anElements.get(i);
                int aValue = anXmlBrowser.getIntValue(anElement.myValueStartPosition, anElement.myValueLength);
                anItems[i] = aValue;
            }
            
            return (T)anItems;
        }
        else if (aDeserializedTypeName.equals("long[]") || aDeserializedTypeName.equals("Long[]"))
        {
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            long[] anItems = new long[anElements.size()];
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlBrowser.TElement anElement = anElements.get(i);
                long aValue = anXmlBrowser.getLongValue(anElement.myValueStartPosition, anElement.myValueLength);
                anItems[i] = aValue;
            }
            
            return (T)anItems;
        }
        else if (aDeserializedTypeName.equals("short[]") || aDeserializedTypeName.equals("Short[]"))
        {
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            short[] anItems = new short[anElements.size()];
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlBrowser.TElement anElement = anElements.get(i);
                short aValue = anXmlBrowser.getShortValue(anElement.myValueStartPosition, anElement.myValueLength);
                anItems[i] = aValue;
            }
            
            return (T)anItems;
        }
        else if (aDeserializedTypeName.equals("double[]") || aDeserializedTypeName.equals("Double[]"))
        {
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            double[] anItems = new double[anElements.size()];
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlBrowser.TElement anElement = anElements.get(i);
                double aValue = anXmlBrowser.getDoubleValue(anElement.myValueStartPosition, anElement.myValueLength);
                anItems[i] = aValue;
            }
            
            return (T)anItems;
        }
        else if (aDeserializedTypeName.equals("float[]") || aDeserializedTypeName.equals("Float[]"))
        {
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            float[] anItems = new float[anElements.size()];
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlBrowser.TElement anElement = anElements.get(i);
                float aValue = anXmlBrowser.getFloatValue(anElement.myValueStartPosition, anElement.myValueLength);
                anItems[i] = aValue;
            }
            
            return (T)anItems;
        }
        else if (aDeserializedTypeName.equals("String[]"))
        {
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            String[] anItems = new String[anElements.size()];
            for (int i = 0; i < anItems.length; ++i)
            {
                XmlBrowser.TElement anElement = anElements.get(i);
                String aValue = anXmlBrowser.getStringValue(anElement.myValueStartPosition, anElement.myValueLength);
                anItems[i] = aValue;
            }
            
            return (T)anItems;
        }
        else
        {
            // Get value of the element.
            ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
            
            // Create the instance of deserialized object via default constructor.
            T aDeserializedObject = (T) clazz.newInstance();
            
            // Get public fields of the deserialized object.
            Field[] aFields = aDeserializedObject.getClass().getFields();
            
            // Go through fields and deserialize them.
            int aSearchIdx = 0;
            for (Field aField : aFields)
            {
                String aFieldName = aField.getName();
                
                // Find the element containing the value.
                // Note: To avoid the looping complexity - searching again and again from the very beginning,
                //       the loop behaves as cycle. The last position is remembered and the
                //       next search starts from this position.
                XmlBrowser.TElement anElement = null;
                for (int aSearchedLength = 0; aSearchedLength < anElements.size(); ++aSearchedLength)
                {
                    XmlBrowser.TElement anTmpElement = anElements.get(aSearchIdx);
                    
                    if (anTmpElement.myName.equals(aFieldName))
                    {
                        // Store found element.
                        anElement = anTmpElement;
                        
                        // Next search start from the next position.
                        ++aSearchIdx;
                        
                        // If we are at the end, then the next search will start from the beginning.
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
                    throw new IllegalStateException("Class field was not found during the deserialization.");
                }
                
                // Recursively deserialize the object for the field.
                Object aValue = deserializeElement(anXmlBrowser, anElement.myValueStartPosition, anElement.myValueLength, aField.getType());
                
                // Set the created object to the field.
                aDeserializedObject.getClass().getField(aFieldName).set(aDeserializedObject, aValue);
            }
            
            return aDeserializedObject;
        }
        
        
        //return null;
    }
    
    private void buildXml(String xmlElementName, Object dataToSerialize, StringBuilder xmlResult)
            throws Exception
    {
        xmlResult.append("<");
        xmlResult.append(xmlElementName);
        xmlResult.append(">");
        
        if (dataToSerialize == null)
        {
            // Do nothing, just write the end of element tag at the end.
        }
        // If it is string then special characters must be replaced.
        else if (dataToSerialize instanceof String)
        {
            serializeString((String)dataToSerialize, xmlResult);
        }
        // If it is a wrapper of a primitive type then write its value.
        else if (dataToSerialize instanceof Boolean ||
                dataToSerialize instanceof Byte ||
                dataToSerialize instanceof Character ||
                dataToSerialize instanceof Double ||
                dataToSerialize instanceof Float ||
                dataToSerialize instanceof Integer ||
                dataToSerialize instanceof Long ||
                dataToSerialize instanceof Short)
        {
            xmlResult.append(dataToSerialize.toString());
        }
        else if (dataToSerialize instanceof Iterable<?>)
        {
            serializeIterable((Iterable<?>)dataToSerialize, xmlResult);
        }
        else if (dataToSerialize.getClass().isArray())
        {
            serializeArray(dataToSerialize, xmlResult);
        }
        // If it is a class with public members.
        else if (dataToSerialize.getClass().getFields().length > 0)
        {
            // If the object has some members then serialize them.
            Field[] aFields = dataToSerialize.getClass().getFields();
            for (Field aField : aFields)
            {
                buildXml(aField.getName(), aField.get(dataToSerialize), xmlResult);
            }
        }
        else
        {
            //TODO: Not possible to serialize?   
        }
        
        xmlResult.append("</");
        xmlResult.append(xmlElementName);
        xmlResult.append(">");
    }
    
    
    /**
     * Parses the given string and replaces the xml specific characters.
     * @param s
     * @return
     */
    private void serializeString(String s, StringBuilder xmlResult)
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
    
    private void serializeArray(Object array, StringBuilder xmlResult) throws Exception
    {
        if (array instanceof boolean[])
        {
            for (boolean anItem : (boolean[])array)
            {
                serializePrimitive(anItem, xmlResult);
            }
        }
        else if (array instanceof byte[])
        {
            for (byte anItem : (byte[])array)
            {
                serializePrimitive(anItem, xmlResult);
            }
        }
        else if (array instanceof char[])
        {
            for (char anItem : (char[])array)
            {
                serializePrimitive(anItem, xmlResult);
            }
        }
        else if (array instanceof double[])
        {
            for (double anItem : (double[])array)
            {
                serializePrimitive(anItem, xmlResult);
            }
        }
        else if (array instanceof float[])
        {
            for (float anItem : (float[])array)
            {
                serializePrimitive(anItem, xmlResult);
            }
        }
        else if (array instanceof int[])
        {
            for (int anItem : (int[])array)
            {
                serializePrimitive(anItem, xmlResult);
            }
        }
        else if (array instanceof long[])
        {
            for (long anItem : (long[])array)
            {
                serializePrimitive(anItem, xmlResult);
            }
        }
        else if (array instanceof short[])
        {
            for (short anItem : (short[])array)
            {
                serializePrimitive(anItem, xmlResult);
            }
        }
        else if (array instanceof Object[])
        {
            for (Object anItem : (Object[])array)
            {
                buildXml(anItem.getClass().getSimpleName(), anItem, xmlResult);
            }
        }
    }
    
    private <T> void serializeIterable(Iterable<T> iterable, StringBuilder xmlResult) throws Exception
    {
        for (Iterator<T> it = iterable.iterator(); it.hasNext();)
        {
            T anItem = it.next();
            
            buildXml(anItem.getClass().getSimpleName(), anItem, xmlResult);
        }
    }

    
    private void serializePrimitive(boolean item, StringBuilder xmlResult)
    {
        xmlResult.append("<bool>");
        xmlResult.append(item);
        xmlResult.append("</bool>");
    }
    
    private void serializePrimitive(byte item, StringBuilder xmlResult)
    {
        xmlResult.append("<byte>");
        xmlResult.append(item);
        xmlResult.append("</byte>");
    }
    
    private void serializePrimitive(char item, StringBuilder xmlResult)
    {
        xmlResult.append("<char>");
        xmlResult.append(item);
        xmlResult.append("</char>");
    }
    
    private void serializePrimitive(double item, StringBuilder xmlResult)
    {
        xmlResult.append("<double>");
        xmlResult.append(item);
        xmlResult.append("</double>");
    }
    
    private void serializePrimitive(float item, StringBuilder xmlResult)
    {
        xmlResult.append("<float>");
        xmlResult.append(item);
        xmlResult.append("</float>");
    }
    
    private void serializePrimitive(int item, StringBuilder xmlResult)
    {
        xmlResult.append("<int>");
        xmlResult.append(item);
        xmlResult.append("</int>");
    }
    
    private void serializePrimitive(long item, StringBuilder xmlResult)
    {
        xmlResult.append("<long>");
        xmlResult.append(item);
        xmlResult.append("</long>");
    }
    
    private void serializePrimitive(short item, StringBuilder xmlResult)
    {
        xmlResult.append("<short>");
        xmlResult.append(item);
        xmlResult.append("</short>");
    }

    private int skipWhiteCharacters(String xmlString, int startPos)
    {
        int i = startPos;
        for (; i < xmlString.length(); ++i)
        {
            char c = xmlString.charAt(i);
            
            if (c != ' ' && c != '\t' && c != '\n' && c != '\f' && c != '\r')
            {
                // Return the new position.
                return i;
            }
        }
        
        return -1;
    }
    
    
}

package eneter.messaging.dataprocessing.serializing;

import java.lang.reflect.*;
import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.Iterator;

import eneter.messaging.diagnostic.EneterTrace;

public class XmlStringSerializer implements ISerializer
{
    private class XmlBrowser
    {
        public class TElement
        {
            public String myName;
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
                anIdx += anElement.myValueLength;
            }
            
            return anElements;
        }
        
        public TElement getElement(int startIdx)
        {
            // Get the starting of the element. '<'
            int anIdx = getNextNonwhiteChar(startIdx);
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
                else if (c == aBeginningElement.charAt(anEqualToEndingIdx))
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
        
        
        public int getNextNonwhiteChar(int startIdx)
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
        
        public char getChar(int idx)
        {
            if (idx < myXmlString.length())
            {
                char c = myXmlString.charAt(idx);
                return c;
            }
            
            return Character.MIN_VALUE;
        }
        
        public boolean isWhiteCharacter(char c)
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
        // Create the instance of deserialized object via default constructor.
        T aDeserializedObject = clazz.newInstance();
        
        // Get values belonging to the element.
        ArrayList<XmlBrowser.TElement> anElements = anXmlBrowser.getElements(elementStartPosition, length);
        
        // Get public fields of the deserialized object.
        Field[] aFields = aDeserializedObject.getClass().getFields();
        
        // Go through fields and deserialize them.
        for (Field aField : aFields)
        {
            
        }
        
        
        return aDeserializedObject;
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

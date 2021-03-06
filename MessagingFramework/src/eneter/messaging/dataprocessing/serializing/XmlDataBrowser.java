/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Internal helper class for browsing of xml string.
 * The class is used to serialize/deserialize xml strings.
 *
 * Note: Do not use tracing in this class.
 */
class XmlDataBrowser
{
    public class TElement
    {
        public String myName = "";
        public int myValueStartPosition;
        public int myValueLength;
        public int myNextElementStartPosition;
        public boolean myIsNull;
        public Class<?> myClazz;
    }

    public XmlDataBrowser(String xmlString) 
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myXmlString = xmlString;
            
            // Note: the order is important. The further code relays on this order.
            String[] aKeywords = {BOOL_ATTRIBUTE, CHAR_ATTRIBUTE, BYTE_ATTRIBUTE,
                                 SHORT_ATTRIBUTE, INT_ATTRIBUTE, LONG_ATTRIBUTE,
                                 FLOAT_ATTRIBUTE, DOUBLE_ATTRIBUTE,
                                 STRING_ATTRIBUTE,
                                 BYTE_ARRAY_ATTRIBUTE,
                                 NILLATTRIBUTE};
                    
            myKeywordIdentifier = new KeywordIdentifier(aKeywords);
            
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
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

        // Find the end of the element "declaration" and recognize attributes if any.
        myKeywordIdentifier.reset();
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
            
            // Check if there are attributes.
            int aKeywordIdx = myKeywordIdentifier.evaluate(c);
            if (aKeywordIdx > -1)
            {
                // Nil attribute.
                if (aKeywordIdx == 10)
                {
                    anElement.myIsNull = true;
                }
                else if (aKeywordIdx < 10)
                {
                    anElement.myClazz = myClazzes[aKeywordIdx];
                }
            }
        }

        // If the element is null or the element is closed. e.g. <aaa />
        if (anElement.myIsNull || getChar(anIdx - 1) == '/')
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

    public String getStringValue(int startIdx, int length)
    {
        StringBuilder aResult = new StringBuilder(length);

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
                aResult.append(c);
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
                                                aResult.append('&');
                                            } else
                                            {
                                                aResult.append("&amp");
                                                aResult.append(c);
                                            }
                                        } else
                                        {
                                            aResult.append("&amp");
                                        }
                                    } else
                                    {
                                        aResult.append("&am");
                                        aResult.append(c);
                                    }
                                } else
                                {
                                    aResult.append("&am");
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
                                                        aResult.append('\'');
                                                    } else
                                                    {
                                                        aResult.append("&apos");
                                                        aResult.append(c);
                                                    }
                                                } else
                                                {
                                                    aResult.append("&apos");
                                                }
                                            } else
                                            {
                                                aResult.append("&apo");
                                                aResult.append(c);
                                            }
                                        } else
                                        {
                                            aResult.append("&apo");
                                        }
                                    } else
                                    {
                                        aResult.append("&ap");
                                        aResult.append(c);
                                    }
                                } else
                                {
                                    aResult.append("&ap");
                                }
                            } else
                            {
                                aResult.append("&a");
                                aResult.append(c);
                            }
                        } else
                        {
                            aResult.append("&a");
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
                                        aResult.append('<');
                                    } else
                                    {
                                        aResult.append("&lt");
                                        aResult.append(c);
                                    }
                                } else
                                {
                                    aResult.append("&lt");
                                }
                            } else
                            {
                                aResult.append("&l");
                                aResult.append(c);
                            }
                        } else
                        {
                            aResult.append("&l");
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
                                        aResult.append('>');
                                    } else
                                    {
                                        aResult.append("&gt");
                                        aResult.append(c);
                                    }
                                } else
                                {
                                    aResult.append("&gt");
                                }
                            } else
                            {
                                aResult.append("&g");
                                aResult.append(c);
                            }
                        } else
                        {
                            aResult.append("&g");
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
                                                        aResult.append('"');
                                                    } else
                                                    {
                                                        aResult.append("&quot");
                                                        aResult.append(c);
                                                    }
                                                } else
                                                {
                                                    aResult.append("&quot");
                                                }
                                            } else
                                            {
                                                aResult.append("&quo");
                                                aResult.append(c);
                                            }
                                        } else
                                        {
                                            aResult.append("&quo");
                                        }
                                    } else
                                    {
                                        aResult.append("&qu");
                                        aResult.append(c);
                                    }
                                } else
                                {
                                    aResult.append("&qu");
                                }
                            } else
                            {
                                aResult.append("&q");
                                aResult.append(c);
                            }
                        } else
                        {
                            aResult.append("&q");
                        }
                    } else
                    {
                        aResult.append('&');
                        aResult.append(c);
                    }
                } else
                {
                    aResult.append('&');
                }
            }
        }

        return aResult.toString();
    }

    public boolean getBooleanValue(int startIdx, int length)
    {
        String aValueStr = myXmlString.substring(startIdx, startIdx + length);
        return Boolean.parseBoolean(aValueStr);
    }

    public char getCharValue(int startIdx)
    {
        char c = myXmlString.charAt(startIdx);
        return c;
    }
    
    public byte getByteValue(int startIdx, int length)
    {
        String aValueStr = myXmlString.substring(startIdx, startIdx + length);
        return Byte.parseByte(aValueStr);
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

    

    // Note: Do not put trace into this method.
    private boolean isWhiteCharacter(char c)
    {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    private String myXmlString;
    
    private KeywordIdentifier myKeywordIdentifier;
    
    private final String NILLATTRIBUTE = "nil=\"true\"";
    
    private final String BOOL_ATTRIBUTE = ":boolean\"";
    private final String CHAR_ATTRIBUTE = ":char\"";
    private final String BYTE_ATTRIBUTE = ":unsignedByte\"";
    private final String INT_ATTRIBUTE = ":int\"";
    private final String SHORT_ATTRIBUTE = ":short\"";
    private final String LONG_ATTRIBUTE = ":long\"";
    private final String FLOAT_ATTRIBUTE = ":float\"";
    private final String DOUBLE_ATTRIBUTE = ":double\"";
    
    private final String STRING_ATTRIBUTE = ":string\"";
    private final String BYTE_ARRAY_ATTRIBUTE = ":base64Binary\"";
    
    private Class<?>[] myClazzes = {boolean.class, char.class, byte.class,
                                    short.class, int.class, long.class,
                                    float.class, double.class,
                                    String.class,
                                    byte[].class};
  
    
}

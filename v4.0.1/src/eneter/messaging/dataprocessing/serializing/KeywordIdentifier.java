/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

/**
 * Internal helper class to find desired keywords from the sequence of chars.
 * It is very fast search utility to identify desired strings from the sequence of chars.
 *
 * Note: EneterTrace is not used because this functionality is called very often
 *       the trace would not be readable.
 */
class KeywordIdentifier
{
    private class TKeyword
    {
        public TKeyword(String keyword)
        {
            myKeyword = keyword;
            myIdx = 0;
        }
        
        public boolean evaluate(char c)
        {
            boolean aMatchFlag = myKeyword.charAt(myIdx) == c;

            if (aMatchFlag)
            {
                // The next character should match with the next position in the string.
                ++myIdx;
                
                // If it was the last character in the keyword, then the this keyword
                // was identified.
                if (myIdx >= myKeyword.length())
                {
                    myIdx = 0;
                    return true;
                }
            }
            else
            {
                myIdx = 0;
            }
            
            return false;
        }

        // Resets the identification process.
        public void reset()
        {
            myIdx = 0;
        }
        
        private int myIdx;
        private final String myKeyword;
    }
    
    public KeywordIdentifier(String[] keywords)
    {
        myKeywords = new TKeyword[keywords.length];
        for (int i = 0; i < keywords.length; ++i)
        {
            myKeywords[i] = new TKeyword(keywords[i]);
        }
    }
    
    public void reset()
    {
        for (TKeyword aKeyword : myKeywords)
        {
            aKeyword.reset();
        }
    }
    
    /**
     * Is called to evaluate the next character in the char sequence.
     * @param c
     * @return if a keyword was identified, it returns the index of the keyword.
     */
    public int evaluate(char c)
    {
        for (int i = 0; i < myKeywords.length; ++i)
        {
            if (myKeywords[i].evaluate(c))
            {
                return i;
            }
        }
        
        return -1;
    }

    private TKeyword[] myKeywords;
}

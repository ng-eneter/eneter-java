package eneter.messaging.dataprocessing.serializing;

import eneter.messaging.diagnostic.EneterTrace;

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
            EneterTrace aTrace = EneterTrace.entering();
            try
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
            finally
            {
                EneterTrace.leaving(aTrace);
            }
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
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myKeywords = new TKeyword[keywords.length];
            for (int i = 0; i < keywords.length; ++i)
            {
                myKeywords[i] = new TKeyword(keywords[i]);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
        
    }
    
    public void reset()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            for (TKeyword aKeyword : myKeywords)
            {
                aKeyword.reset();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public int evaluate(char c)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
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
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private TKeyword[] myKeywords;
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.io.*;

import eneter.messaging.diagnostic.EneterTrace;

public final class WebSocketMessage
{
    WebSocketMessage(boolean isText, InputStream inputStream)
    {
        myIsText = isText;
        myInputStream = inputStream;
    }

    public byte[] getWholeMessage() throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream anOutputMemStream = new ByteArrayOutputStream();
            try
            {
                int aSize = 0;
                byte[] aBuffer = new byte[32768];
                while ((aSize = myInputStream.read(aBuffer, 0, aBuffer.length)) > 0)
                {
                    anOutputMemStream.write(aBuffer, 0, aSize);
                }
            }
            finally
            {
                anOutputMemStream.close();
            }
            
            return anOutputMemStream.toByteArray();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }

    }
    
    public String GetWholeTextMessage() throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (!myIsText)
            {
                throw new IllegalStateException("This is not text message. WebSocketMessage.IsText != true.");
            }

            byte[] aMessageContent = getWholeMessage();
            
            String aMessageText = new String(aMessageContent, "UTF-8");

            return aMessageText;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public boolean IsText()
    {
        return myIsText;
    }
    
    public InputStream getInputStream()
    {
        return myInputStream;
    }
  
    private boolean myIsText;
    private InputStream myInputStream;
}

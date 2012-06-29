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

/**
 * Represents a data message received via websocket communication.
 *
 */
public final class WebSocketMessage
{
    /**
     * Constructs the message - internal constructor used by the eneter framework.
     * @param isText true if it is a text message.
     * @param inputStream
     */
    WebSocketMessage(boolean isText, InputStream inputStream)
    {
        myIsText = isText;
        myInputStream = inputStream;
    }

    /**
     * Returns the whole incoming message.
     * In case the message was sent via multiple frames it waits until all frames are
     * collected and then returns the result message.
     * @return received message
     * @throws IOException
     */
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
    
    /**
     * Returns the whole incoming text message.
     * In case the message was sent via multiple frames it waits until all frames are
     * collected and then returns the result message.<br/>
     * To receive message as a text message, according to websocket protocol the message
     * must be sent via the text frame.
     * @return received text message
     * @throws IOException
     */
    public String getWholeTextMessage() throws IOException
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
    
    /**
     * Returns true if the message is text. The message is text when sent via text frame.
     * @return
     */
    public boolean isText()
    {
        return myIsText;
    }
    
    /**
     * Returns the input stream user can use to read the message from.
     * The reading of the stream blocks if desired amount of data is not available and
     * not all message frames were received.
     * @return
     */
    public InputStream getInputStream()
    {
        return myInputStream;
    }
  
    private boolean myIsText;
    private InputStream myInputStream;
}

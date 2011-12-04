package eneter.messaging.dataprocessing.streaming;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

public class Test_MessageStreamer
{
    @Test
    public void writeReadMessage() throws Exception
    {
        String aMessage = "Hello";
        
        // Store the message in the buffer
        byte[] aStreamedMessage;
        ByteArrayOutputStream aMemStream = new ByteArrayOutputStream();
        try
        {
            MessageStreamer.writeMessage(aMemStream, aMessage);
            aStreamedMessage = aMemStream.toByteArray();
        }
        finally
        {
            aMemStream.close();
        }
        
        Object aReadMessage;
        ByteArrayInputStream anInputMemStream = new ByteArrayInputStream(aStreamedMessage);
        try
        {
            aReadMessage = MessageStreamer.readMessage(anInputMemStream);
        }
        finally
        {
            aMemStream.close();
        }

        assertTrue(aReadMessage instanceof String);
        assertEquals(aMessage, (String)aReadMessage);
    }
}

package eneter.messaging.messagingsystems;

import static org.junit.Assert.*;

import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.IMethod2;
import eneter.net.system.threading.ManualResetEvent;

public abstract class MessagingSystemBaseTester
{
    public MessagingSystemBaseTester()
    {
        myChannelId = "Channel1";
    }
    
    @Test
    public void A01_SendMessage()
            throws Exception
    {
        sendMessageViaOutputChannel(myChannelId, "Message", 1, 5000);
    }
    
    @Test
    public void A02_SendMessages500()
            throws Exception
    {
        sendMessageViaOutputChannel(myChannelId, "Message", 500, 5000);
    }
    
    
    private void sendMessageViaOutputChannel(final String channelId, final Object message, final int numberOfTimes, int timeOutForMessageProcessing)
            throws Exception
    {
        IOutputChannel anOutputChannel = myMessagingSystemFactory.createOutputChannel(channelId);
        IInputChannel anInputChannel = myMessagingSystemFactory.createInputChannel(channelId);

        final ManualResetEvent aMessagesSentEvent = new ManualResetEvent(false);

        IMethod2<Object, ChannelMessageEventArgs> aMessageReceivedEventHandler = new IMethod2<Object, ChannelMessageEventArgs>()
        {
            @Override
            public void invoke(Object x, ChannelMessageEventArgs y)
            {
                // Some messaging system can have a parallel access therefore we must ensure
                // that results are put to the list synchronously.
                synchronized(myLock)
                {
                    ++myNumberOfReceivedMessages;

                    if (myChannelId != y.getChannelId() || (String)message != (String)y.getMessage())
                    {
                        ++myNumberOfFailures;
                    }

                    // Release helper thread when all messages are received.
                    if (myNumberOfReceivedMessages == numberOfTimes)
                    {
                        aMessagesSentEvent.set();
                    }
                }
            }
            
            // These 2 values will be read by using the reflection - only for testing purposes. 
            private int myNumberOfReceivedMessages;
            private int myNumberOfFailures;
            
            private Object myLock = new Object(); 
        };
        
        
        
        anInputChannel.messageReceived().subscribe(aMessageReceivedEventHandler);

        try
        {
            anInputChannel.startListening();

            // Send messages
            for (int i = 0; i < numberOfTimes; ++i)
            {
                anOutputChannel.sendMessage(message);
            }

            EneterTrace.info("Send messages to '" + myChannelId + "' completed - waiting while they are processed.");

            // Wait until all messages are processed.
            //assertTrue(aMessagesSentEvent.waitOne(timeOutForMessageProcessing));
            //Assert.IsTrue(aMessagesSentEvent.WaitOne());
            aMessagesSentEvent.waitOne();

            EneterTrace.info("Waiting for processing of messages on '" + myChannelId + "' completed.");
        }
        finally
        {
            anInputChannel.stopListening();
        }

        // Let's use reflection to read values from the anonymous class.
        // Note: Using reflection is only for the testing purposes.
        int aNumberOfFails = aMessageReceivedEventHandler.getClass().getField("myNumberOfFailures").getInt(aMessageReceivedEventHandler);
        int aNumberOfReceived = aMessageReceivedEventHandler.getClass().getField("myNumberOfReceivedMessages").getInt(aMessageReceivedEventHandler);
        
        assertEquals(0, aNumberOfFails);
        assertEquals(numberOfTimes, aNumberOfReceived);
    }
    
    

    protected IMessagingSystemFactory myMessagingSystemFactory;
    protected String myChannelId;
}

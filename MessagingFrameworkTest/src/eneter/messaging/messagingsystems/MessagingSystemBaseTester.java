package eneter.messaging.messagingsystems;

import static org.junit.Assert.*;

import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.IMethod2;
import eneter.net.system.threading.*;

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
    
    @Test
    public void A05_SendMessageReceiveResponse()
            throws Exception
    {
        sendMessageReceiveResponse(myChannelId, "Message", "Response", 1, 1000);
    }
    
    @Test
    public void A06_SendMessageReceiveResponse500()
            throws Exception
    {
        sendMessageReceiveResponse(myChannelId, "Message", "Respones", 500, 1000);
    }
    
    @Test(expected = IllegalStateException.class)
    public void A07_StopListening()
        throws Exception
    {
        IOutputChannel anOutputChannel = myMessagingSystemFactory.createOutputChannel(myChannelId);
        IInputChannel anInputChannel = myMessagingSystemFactory.createInputChannel(myChannelId);

        anInputChannel.messageReceived().subscribe(new IMethod2<Object, ChannelMessageEventArgs>()
        {
            @Override
            public void invoke(Object t1, ChannelMessageEventArgs t2) throws Exception
            {
                // This method should not be never executed.
                // So we are not interested in received messages.
            }
        });

        anInputChannel.startListening();

        Thread.sleep(100);

        anInputChannel.stopListening();

        // Send the message. Since the listening is stopped nothing should be delivered.
        anOutputChannel.sendMessage("Message");

        Thread.sleep(500);
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
            public int myNumberOfReceivedMessages;
            public int myNumberOfFailures;
            
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
    
    
    private void sendMessageReceiveResponse(final String channelId, final Object message, final Object resonseMessage, final int numberOfTimes, int timeOutForMessageProcessing)
            throws Exception
    {
        IDuplexOutputChannel anOutputChannel = myMessagingSystemFactory.CreateDuplexOutputChannel(channelId);
        final IDuplexInputChannel anInputChannel = myMessagingSystemFactory.CreateDuplexInputChannel(channelId);

        final AutoResetEvent aMessagesSentEvent = new AutoResetEvent(false);

        IMethod2<Object, DuplexChannelMessageEventArgs> aMessageReceivedHandler = new IMethod2<Object, DuplexChannelMessageEventArgs>()
        {
            @Override
            public void invoke(Object x, DuplexChannelMessageEventArgs y)
                    throws Exception
            {
                // Some messaging system can have a parallel access therefore we must ensure
                // that results are put to the list synchronously.
                synchronized (amyMessageReceiverLock)
                {
                    ++amyNumberOfReceivedMessages;

                    if (channelId != y.getChannelId() || (String)message != (String)y.getMessage())
                    {
                        ++amyNumberOfFailedMessages;
                    }
                    else
                    {
                        // everything is ok -> send the response
                        anInputChannel.sendResponseMessage(y.getResponseReceiverId(), resonseMessage);
                    }
                }
            }
            
            private Object amyMessageReceiverLock = new Object();
            public int amyNumberOfReceivedMessages;
            public int amyNumberOfFailedMessages;
        };
        
        anInputChannel.messageReceived().subscribe(aMessageReceivedHandler);
        
        
        IMethod2<Object, DuplexChannelMessageEventArgs> aResponseReceivedHandler = new IMethod2<Object, DuplexChannelMessageEventArgs>()
        {
            @Override
            public void invoke(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (amyResponseReceiverLock)
                {
                    ++amyNumberOfReceivedResponses;
                    if ((String)resonseMessage != (String)y.getMessage())
                    {
                        ++amyNumberOfFailedResponses;
                    }

                    // Release helper thread when all messages are received.
                    if (amyNumberOfReceivedResponses == numberOfTimes)
                    {
                        aMessagesSentEvent.set();
                    }
                }
            }
            
            private Object amyResponseReceiverLock = new Object();
            public int amyNumberOfReceivedResponses;
            public int amyNumberOfFailedResponses;
        };

        anOutputChannel.responseMessageReceived().subscribe(aResponseReceivedHandler);

        try
        {
            // Input channel starts listening
            anInputChannel.startListening();

            // Output channel connects in order to be able to receive response messages.
            anOutputChannel.openConnection();

            // Send messages
            for (int i = 0; i < numberOfTimes; ++i)
            {
                anOutputChannel.sendMessage(message);
            }

            EneterTrace.info("Send messages to '" + myChannelId + "' completed - waiting while they are processed.");

            // Wait until all messages are processed.
            //Assert.IsTrue(aMessagesSentEvent.WaitOne(timeOutForMessageProcessing));
            //AssertTrue(aMessagesSentEvent.WaitOne());
            aMessagesSentEvent.waitOne();

            EneterTrace.info("Waiting for processing of messages on '" + myChannelId + "' completed.");
        }
        finally
        {
            try
            {
                anOutputChannel.closeConnection();
            }
            finally
            {
                anInputChannel.stopListening();
            }
        }

        // Let's use reflection to read values from the anonymous class.
        // Note: Using reflection is only for the testing purposes.
        int aNumberOfFailedMessages = aMessageReceivedHandler.getClass().getField("amyNumberOfFailedMessages").getInt(aMessageReceivedHandler);
        int aNumberOfFailedResponses = aResponseReceivedHandler.getClass().getField("amyNumberOfFailedResponses").getInt(aResponseReceivedHandler);
        
        int aNumberOfReceivedMessages = aMessageReceivedHandler.getClass().getField("amyNumberOfReceivedMessages").getInt(aMessageReceivedHandler);
        int aNumberOfReceivedResponses = aResponseReceivedHandler.getClass().getField("amyNumberOfReceivedResponses").getInt(aResponseReceivedHandler);
        
        assertEquals("There are failed messages.", 0, aNumberOfFailedMessages);
        assertEquals("There are failed response messages.", 0, aNumberOfFailedResponses);
        assertEquals("Number of sent messages differs from number of received.", numberOfTimes, aNumberOfReceivedMessages);
        assertEquals("Number of received responses differs from number of sent responses.", numberOfTimes, aNumberOfReceivedResponses);
    }
    

    protected IMessagingSystemFactory myMessagingSystemFactory;
    protected String myChannelId;
}

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

        final int[] aNumberOfReceivedMessages = new int[1];
        final int[] aNumberOfFailures = new int[1];
        
        IMethod2<Object, ChannelMessageEventArgs> aMessageReceivedEventHandler = new IMethod2<Object, ChannelMessageEventArgs>()
        {
            @Override
            public void invoke(Object x, ChannelMessageEventArgs y)
            {
                // Some messaging system can have a parallel access therefore we must ensure
                // that results are put to the list synchronously.
                synchronized(myLock)
                {
                    ++aNumberOfReceivedMessages[0];

                    if (myChannelId != y.getChannelId() || (String)message != (String)y.getMessage())
                    {
                        ++aNumberOfFailures[0];
                    }

                    // Release helper thread when all messages are received.
                    if (aNumberOfReceivedMessages[0] == numberOfTimes)
                    {
                        aMessagesSentEvent.set();
                    }
                }
            }
            
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
            assertTrue(aMessagesSentEvent.waitOne(timeOutForMessageProcessing));

            EneterTrace.info("Waiting for processing of messages on '" + myChannelId + "' completed.");
        }
        finally
        {
            anInputChannel.stopListening();
        }

        assertEquals(0, aNumberOfFailures[0]);
        assertEquals(numberOfTimes, aNumberOfReceivedMessages[0]);
    }
    
    
    private void sendMessageReceiveResponse(final String channelId, final Object message, final Object resonseMessage, final int numberOfTimes, int timeOutForMessageProcessing)
            throws Exception
    {
        IDuplexOutputChannel anOutputChannel = myMessagingSystemFactory.CreateDuplexOutputChannel(channelId);
        final IDuplexInputChannel anInputChannel = myMessagingSystemFactory.CreateDuplexInputChannel(channelId);

        final AutoResetEvent aMessagesSentEvent = new AutoResetEvent(false);

        final int[] aNumberOfReceivedMessages = new int[1];
        final int[] aNumberOfFailedMessages = new int[1];
        
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
                    ++aNumberOfReceivedMessages[0];

                    if (channelId != y.getChannelId() || (String)message != (String)y.getMessage())
                    {
                        ++aNumberOfFailedMessages[0];
                    }
                    else
                    {
                        // everything is ok -> send the response
                        anInputChannel.sendResponseMessage(y.getResponseReceiverId(), resonseMessage);
                    }
                }
            }
            
            private Object amyMessageReceiverLock = new Object();
        };
        
        anInputChannel.messageReceived().subscribe(aMessageReceivedHandler);
        
        
        final int[] aNumberOfReceivedResponses = new int[1];
        final int[] aNumberOfFailedResponses = new int[1];
        
        IMethod2<Object, DuplexChannelMessageEventArgs> aResponseReceivedHandler = new IMethod2<Object, DuplexChannelMessageEventArgs>()
        {
            @Override
            public void invoke(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (amyResponseReceiverLock)
                {
                    ++aNumberOfReceivedResponses[0];
                    if ((String)resonseMessage != (String)y.getMessage())
                    {
                        ++aNumberOfFailedResponses[0];
                    }

                    // Release helper thread when all messages are received.
                    if (aNumberOfReceivedResponses[0] == numberOfTimes)
                    {
                        aMessagesSentEvent.set();
                    }
                }
            }
            
            private Object amyResponseReceiverLock = new Object();
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
            assertTrue(aMessagesSentEvent.waitOne(timeOutForMessageProcessing));

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

        assertEquals("There are failed messages.", 0, aNumberOfFailedMessages[0]);
        assertEquals("There are failed response messages.", 0, aNumberOfFailedResponses[0]);
        assertEquals("Number of sent messages differs from number of received.", numberOfTimes, aNumberOfReceivedMessages[0]);
        assertEquals("Number of received responses differs from number of sent responses.", numberOfTimes, aNumberOfReceivedResponses[0]);
    }
    

    protected IMessagingSystemFactory myMessagingSystemFactory;
    protected String myChannelId;
}

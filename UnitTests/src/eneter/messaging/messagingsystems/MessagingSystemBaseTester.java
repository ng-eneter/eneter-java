package eneter.messaging.messagingsystems;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.internal.*;

public abstract class MessagingSystemBaseTester
{
    private static class TDuplexClient
    {
        public TDuplexClient(IMessagingSystemFactory messaging, String channelId, String expectedResponseMessage,
                int expectedNumberOfResponseMessages) throws Exception
            {
                OutputChannel = messaging.createDuplexOutputChannel(channelId);
                OutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);

                ConnectionOpenEvent = new ManualResetEvent(false);
                ResponsesReceivedEvent = new ManualResetEvent(false);
                myExpectedResponseMessage = expectedResponseMessage;
                myExpectedNumberOfResponses = expectedNumberOfResponseMessages;
            }
        
        public void openConnection() throws Exception
        {
            OutputChannel.openConnection();
            ConnectionOpenEvent.set();
        }
        
        private void onResponseMessageReceived(DuplexChannelMessageEventArgs e)
        {
            synchronized(myResponseReceiverLock)
            {
                ++NumberOfReceivedResponses;

                //EneterTrace.info("Received Responses: " + NumberOfReceivedResponses);

                if (myExpectedResponseMessage.equals((String)e.getMessage()) == false)
                {
                    ++NumberOfFailedResponses;
                }

                // Release helper thread when all messages are received.
                if (NumberOfReceivedResponses == myExpectedNumberOfResponses)
                {
                    ResponsesReceivedEvent.set();
                }
            }
        }
        
        public int NumberOfReceivedResponses;
        public int NumberOfFailedResponses;
        public IDuplexOutputChannel OutputChannel;

        public ManualResetEvent ConnectionOpenEvent;
        public ManualResetEvent ResponsesReceivedEvent;

        private int myExpectedNumberOfResponses;
        private String myExpectedResponseMessage;
        private Object myResponseReceiverLock = new Object();
        
        
        private EventHandler<DuplexChannelMessageEventArgs> myOnResponseMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
            {
                onResponseMessageReceived(e);
            }
        };
    }
    
    private static class TDuplexService
    {
        public TDuplexService(IMessagingSystemFactory messaging, String channelId, String expextedMessage,
                int expextedNumberOfMessages,
                String responseMessage) throws Exception
            {
                InputChannel = messaging.createDuplexInputChannel(channelId);
                InputChannel.messageReceived().subscribe(myOnMessageReceived);

                MessagesReceivedEvent = new ManualResetEvent(false);

                myExpectedMessage = expextedMessage;
                myExpectedNumberOfMessages = expextedNumberOfMessages;
                myResponseMessage = responseMessage;
            }
        
        private void onMessageReceived(DuplexChannelMessageEventArgs e) throws Exception
        {
            // Some messaging system can have a parallel access therefore we must ensure
            // that results are put to the list synchronously.
            synchronized (myLock)
            {
                ++NumberOfReceivedMessages;

                //EneterTrace.info("Received Messages: " + NumberOfReceivedMessages);

                if (NumberOfReceivedMessages == myExpectedNumberOfMessages)
                {
                    MessagesReceivedEvent.set();
                }

                if (InputChannel.getChannelId().equals(e.getChannelId()) == false ||
                    myExpectedMessage.equals((String)e.getMessage()) == false)
                {
                    ++NumberOfFailedMessages;
                }
                else
                {
                    // everything is ok -> send the response
                    InputChannel.sendResponseMessage(e.getResponseReceiverId(), myResponseMessage);
                }
            }
        }
        
        public IDuplexInputChannel InputChannel;
        public ManualResetEvent MessagesReceivedEvent;

        public int NumberOfReceivedMessages;
        public int NumberOfFailedMessages;

        private int myExpectedNumberOfMessages;
        private String myExpectedMessage;
        private String myResponseMessage;
        private Object myLock = new Object();
        
        private EventHandler<DuplexChannelMessageEventArgs> myOnMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
            {
                try
                {
                    onMessageReceived(e);
                }
                catch (Exception err)
                {
                    EneterTrace.error("onMessageReceived failed.", err);
                }
            }
        };
    }
    
    
    
    public MessagingSystemBaseTester()
    {
        myChannelId = "Channel1";
    }
    
    
    @Test
    public void Duplex_01_Send1()
            throws Exception
    {
        sendMessageReceiveResponse(myChannelId, "Message", "Response", 1, 1);
    }
    
    @Test
    public void Duplex_02_Send500()
            throws Exception
    {
        sendMessageReceiveResponse(myChannelId, "Message", "Respones", 1, 500);
    }
    
    @Ignore
    @Test
    public void Duplex_03_Send100_10MB() throws Exception
    {
        sendMessageReceiveResponse(myChannelId, myMessage_10MB, myMessage_10MB, 1, 100);
    }
    
    @Test
    public void Duplex_04_Send50000() throws Exception
    {
        sendMessageReceiveResponse(myChannelId, "Message", "Respones", 1, 50000);
    }
    
    @Test
    public void Duplex_05_Send50_10Prallel()
            throws Exception
    {
        sendMessageReceiveResponse(myChannelId, "Message", "Respones", 10, 50);
    }
    
    @Test
    public void Duplex_06_OpenCloseConnection()
        throws Throwable
    {
        IDuplexInputChannel anInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        IDuplexOutputChannel anOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        final AutoResetEvent aResponseReceiverConnectedEvent = new AutoResetEvent(false);
        final String[] aConnectedReceiver = new String[1];
        anInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aConnectedReceiver[0] = y.getResponseReceiverId();

                aResponseReceiverConnectedEvent.set();
            }
        });

        final AutoResetEvent aResponseReceiverDisconnectedEvent = new AutoResetEvent(false);
        final String[] aDisconnectedReceiver = new String[1];
        anInputChannel.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aDisconnectedReceiver[0] = y.getResponseReceiverId();

                aResponseReceiverDisconnectedEvent.set();
            }
        });

        final AutoResetEvent aConnectionOpenedEvent = new AutoResetEvent(false);
        final DuplexChannelEventArgs[] aConnectionOpenedEventArgs = new DuplexChannelEventArgs[1];
        anOutputChannel.connectionOpened().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelEventArgs y)
            {
                aConnectionOpenedEventArgs[0] = y;
                aConnectionOpenedEvent.set();
            }
        });

        final AutoResetEvent aConnectionClosedEvent = new AutoResetEvent(false);
        final DuplexChannelEventArgs[] aConnectionClosedEventArgs = new DuplexChannelEventArgs[1];
        anOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelEventArgs y)
            {
                aConnectionClosedEventArgs[0] = y;
                aConnectionClosedEvent.set();
            }
        });


        try
        {
            anInputChannel.startListening();
            
            anOutputChannel.openConnection();
            assertTrue(anOutputChannel.isConnected());

            aConnectionOpenedEvent.waitOne();
            assertEquals(anOutputChannel.getChannelId(), aConnectionOpenedEventArgs[0].getChannelId());
            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectionOpenedEventArgs[0].getResponseReceiverId());

            aResponseReceiverConnectedEvent.waitOne();
            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectedReceiver[0]);


            anOutputChannel.closeConnection();
            assertFalse(anOutputChannel.isConnected());
            
            aConnectionClosedEvent.waitOne();
            assertEquals(anOutputChannel.getChannelId(), aConnectionClosedEventArgs[0].getChannelId());
            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectionClosedEventArgs[0].getResponseReceiverId());

            aResponseReceiverDisconnectedEvent.waitOne();
            assertEquals(anOutputChannel.getResponseReceiverId(), aDisconnectedReceiver[0]);
        }
        catch (Throwable err)
        {
            anOutputChannel.closeConnection();
            throw err;
        }
        finally
        {
            anInputChannel.stopListening();
        }
        
        assertTrue(!aConnectedReceiver[0].equals(""));
        assertTrue(!aDisconnectedReceiver[0].equals(""));
        assertEquals(aConnectedReceiver[0], aDisconnectedReceiver[0]);
    }
    

    @Test
    public void Duplex_06_OpenCloseOpenSend() throws Exception
    {
        final IDuplexInputChannel anInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        IDuplexOutputChannel anOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        final AutoResetEvent aResponseReceiverConnectedEvent = new AutoResetEvent(false);
        final String[] aConnectedReceiver = { "" };
        anInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aConnectedReceiver[0] = y.getResponseReceiverId();

                aResponseReceiverConnectedEvent.set();
            }
        });
        
        final AutoResetEvent aResponseReceiverDisconnectedEvent = new AutoResetEvent(false);
        final String[] aDisconnectedReceiver = { "" };
        anInputChannel.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aDisconnectedReceiver[0] = y.getResponseReceiverId();

                aResponseReceiverDisconnectedEvent.set();
            }
        });

        final AutoResetEvent aRequestMessageReceivedEvent = new AutoResetEvent(false);
        anInputChannel.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aRequestMessageReceivedEvent.set();

                try
                {
                    // send back the response.
                    anInputChannel.sendResponseMessage(y.getResponseReceiverId(), "Hi");
                }
                catch (Exception err)
                {
                    EneterTrace.error("Sending of response message failed.", err);
                }
            }
        });
        
        final AutoResetEvent aConnectionOpenedEvent = new AutoResetEvent(false);
        final DuplexChannelEventArgs[] aConnectionOpenedEventArgs = { null };
        anOutputChannel.connectionOpened().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelEventArgs y)
            {
                aConnectionOpenedEventArgs[0] = y;
                aConnectionOpenedEvent.set();
            }
        });
        
        final AutoResetEvent aConnectionClosedEvent = new AutoResetEvent(false);
        final DuplexChannelEventArgs[] aConnectionClosedEventArgs = { null };
        anOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelEventArgs y)
            {
                aConnectionClosedEventArgs[0] = y;
                aConnectionClosedEvent.set();
            }
        });
        
        final AutoResetEvent aResponseReceivedEvent = new AutoResetEvent(false);
        anOutputChannel.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
            {
                aResponseReceivedEvent.set();
            }
        });
        
        try
        {
            anInputChannel.startListening();

            // Client opens the connection.
            EneterTrace.debug("Open1");
            anOutputChannel.openConnection();
            assertTrue(anOutputChannel.isConnected());

            // handling open connection on the client side.
            aConnectionOpenedEvent.waitOne();
            assertEquals(anOutputChannel.getChannelId(), aConnectionOpenedEventArgs[0].getChannelId());
            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectionOpenedEventArgs[0].getResponseReceiverId());

            // handling open connection on the service side.
            aResponseReceiverConnectedEvent.waitOne();
            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectedReceiver[0]);


            // Client closes the connection.
            EneterTrace.debug("Close1");
            anOutputChannel.closeConnection();
            assertFalse(anOutputChannel.isConnected());

            aConnectionClosedEvent.waitOne();
            assertEquals(anOutputChannel.getChannelId(), aConnectionClosedEventArgs[0].getChannelId());
            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectionClosedEventArgs[0].getResponseReceiverId());

            aResponseReceiverDisconnectedEvent.waitOne();
            assertEquals(anOutputChannel.getResponseReceiverId(), aDisconnectedReceiver[0]);

            // Messaging system e.g. MessageBus can be more complex and needs some time to really close the connection.
            // Therefore give some time. Investigate how to improve it.
            Thread.sleep(500);

            // Client opens the connection.
            EneterTrace.debug("Open2");
            anOutputChannel.openConnection();
            assertTrue(anOutputChannel.isConnected());

            // handling open connection on the client side.
            aConnectionOpenedEvent.waitOne();
            assertEquals(anOutputChannel.getChannelId(), aConnectionOpenedEventArgs[0].getChannelId());
            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectionOpenedEventArgs[0].getResponseReceiverId());

            // handling open connection on the service side.
            aResponseReceiverConnectedEvent.waitOne();
            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectedReceiver[0]);


            // Client sends a message.
            anOutputChannel.sendMessage("Hello");

            aRequestMessageReceivedEvent.waitOne();
            aResponseReceivedEvent.waitOne();
        }
        finally
        {
            anOutputChannel.closeConnection();
            anInputChannel.stopListening();
        }
    }

    @Test
    public void Duplex_07_OpenConnection_if_InputChannelNotStarted() throws Exception
    {
        IDuplexOutputChannel anOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        try
        {
            anOutputChannel.openConnection();
        }
        catch (Exception err)
        {
        }

        assertFalse(anOutputChannel.isConnected());
    }
    
    @Test
    public void Duplex_08_OpenFromConnectionClosed()
        throws Exception
    {
        final IDuplexInputChannel aDuplexInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        final IDuplexOutputChannel aDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);


        final AutoResetEvent aConnectionReopenEvent = new AutoResetEvent(false);
        final boolean[] isConnected = {true};
        final boolean[] isConnectedAfter = {false};
        aDuplexOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object t1, DuplexChannelEventArgs t2)
            {
                if (!isConnectedAfter[0])
                {
                    // it is disconnected, so false is expected.
                    isConnected[0] = aDuplexOutputChannel.isConnected();

                    // Try to open from the handler.
                    try
                    {
                        aDuplexOutputChannel.openConnection();
                        isConnectedAfter[0] = true;
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error("Open connection failed.", err);
                    }

                    aConnectionReopenEvent.set();
                }
            }
        });
        
        try
        {
            aDuplexInputChannel.startListening();

            aDuplexOutputChannel.openConnection();

            Thread.sleep(500);

            aDuplexInputChannel.disconnectResponseReceiver(aDuplexOutputChannel.getResponseReceiverId());

            aConnectionReopenEvent.waitOne();
        }
        finally
        {
            aDuplexInputChannel.stopListening();
            aDuplexOutputChannel.closeConnection();
        }

        assertFalse(isConnected[0]);
        assertTrue(isConnectedAfter[0]);
    }
    
    @Test
    public void Duplex_09_StopListening_SendMessage() throws Exception
    {
        IDuplexInputChannel anInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        
        final AutoResetEvent aResponseReceiverConnectedEvent = new AutoResetEvent(false);
        anInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aResponseReceiverConnectedEvent.set();
            }
        });
        
        IDuplexOutputChannel anOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        boolean isSomeException = false;

        try
        {
            // Duplex input channel starts to listen.
            anInputChannel.startListening();

            // Duplex output channel connects.
            anOutputChannel.openConnection();
            assertTrue(anOutputChannel.isConnected());

            aResponseReceiverConnectedEvent.waitOne();


            // Duplex input channel stops to listen.
            anInputChannel.stopListening();
            assertFalse(anInputChannel.isListening());

            Thread.sleep(500);

            try
            {
                // Try to send a message via the duplex output channel.
                anOutputChannel.sendMessage("Message");
            }
            catch (Exception err)
            {
                // Because the duplex input channel is not listening the sending must
                // fail with an exception. The type of the exception depends from the type of messaging system.
                isSomeException = true;
            }
        }
        finally
        {
            anOutputChannel.closeConnection();
        }

        assertTrue(isSomeException);
    }
    
    @Test
    public void Duplex_09_StopListeing() throws Exception
    {
        IDuplexInputChannel anInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);

        final AutoResetEvent anAllConnected = new AutoResetEvent(false);
        final int[] aNumber = { 0 };
        anInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object sender, ResponseReceiverEventArgs e)
            {
                ++aNumber[0];
                if (aNumber[0] == 3)
                {
                    anAllConnected.set();
                }
            }
        });
        
        final AutoResetEvent anAllDisconnected = new AutoResetEvent(false);
        final ArrayList<ResponseReceiverEventArgs> aDisconnects = new ArrayList<ResponseReceiverEventArgs>();
        anInputChannel.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                synchronized (aDisconnects)
                {
                    aDisconnects.add(y);

                    if (aDisconnects.size() == 3)
                    {
                        anAllDisconnected.set();
                    }
                }
            }
        });
        
        IDuplexOutputChannel anOutputChannel1 = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);
        IDuplexOutputChannel anOutputChannel2 = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);
        IDuplexOutputChannel anOutputChannel3 = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        try
        {
            // Duplex input channel starts to listen.
            anInputChannel.startListening();

            // Open connections
            anOutputChannel1.openConnection();
            anOutputChannel2.openConnection();
            anOutputChannel3.openConnection();
            assertTrue(anOutputChannel1.isConnected());
            assertTrue(anOutputChannel2.isConnected());
            assertTrue(anOutputChannel3.isConnected());

            anAllConnected.waitOne();

            // Stop listening.
            anInputChannel.stopListening();
            assertFalse(anInputChannel.isListening());

            anAllDisconnected.waitOne();

            // Wait if e.g. more that three disconnects are delivered then error.
            Thread.sleep(200);

            assertEquals(3, aDisconnects.size());
        }
        finally
        {
            anOutputChannel1.closeConnection();
            anOutputChannel2.closeConnection();
            anOutputChannel3.closeConnection();
            anInputChannel.stopListening();
        }
    }
    
    @Test
    public void Duplex_10_DisconnectResponseReceiver()
        throws Exception
    {
        final AutoResetEvent aResponseReceiverConnectedEvent = new AutoResetEvent(false);
        final AutoResetEvent aConnectionClosedEvent = new AutoResetEvent(false);

        final boolean[] aResponseReceiverConnectedFlag = new boolean[1];
        final boolean[] aResponseReceiverDisconnectedFlag = new boolean[1];

        final boolean[] aConnectionClosedReceivedInOutputChannelFlag = new boolean[1];
        final boolean[] aResponseMessageReceivedFlag = new boolean[1];

        // Create duplex input channel.
        IDuplexInputChannel aDuplexInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        aDuplexInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object t1, ResponseReceiverEventArgs t2)
            {
                aResponseReceiverConnectedFlag[0] = true;
                aResponseReceiverConnectedEvent.set();
            }
        });
        
        aDuplexInputChannel.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object t1, ResponseReceiverEventArgs t2)
            {
                aResponseReceiverDisconnectedFlag[0] = true;
            }
        });
        

        // Create duplex output channel.
        IDuplexOutputChannel aDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);
        aDuplexOutputChannel.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object t1, DuplexChannelMessageEventArgs t2)
            {
                aResponseMessageReceivedFlag[0] = true;
            }
        });
        
        aDuplexOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object t1, DuplexChannelEventArgs t2)
            {
                aConnectionClosedReceivedInOutputChannelFlag[0] = true;
                aConnectionClosedEvent.set();
            }
        });
        

        try
        {
            aDuplexInputChannel.startListening();
            aDuplexOutputChannel.openConnection();

            // Wait until the connection is established.
            aResponseReceiverConnectedEvent.waitOne();

            // Disconnect response receiver from the duplex input channel.
            aDuplexInputChannel.disconnectResponseReceiver(aDuplexOutputChannel.getResponseReceiverId());

            // Wait until the response receiver is disconnected.
            aConnectionClosedEvent.waitOne();
            
            assertTrue(aResponseReceiverConnectedFlag[0]);
            assertTrue(aConnectionClosedReceivedInOutputChannelFlag[0]);

            assertFalse(aResponseMessageReceivedFlag[0]);
            
            // Disconnect response receiver shall generate the client disconnected event.
            assertTrue(aResponseReceiverDisconnectedFlag[0]);
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }
    }
    
    @Test
    public void Duplex_11_CloseConnection()
        throws Exception
    {
        IDuplexInputChannel aDuplexInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);

        IDuplexOutputChannel aDuplexOutputChannel1 = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);
        IDuplexOutputChannel aDuplexOutputChannel2 = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        final AutoResetEvent aResponseReceiverConnectedEvent = new AutoResetEvent(false);
        final String[] aConnectedResponseReceiver = new String[1];
        aDuplexInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aConnectedResponseReceiver[0] = y.getResponseReceiverId();
                aResponseReceiverConnectedEvent.set();
            }
        });
        
        final AutoResetEvent aResponseReceiverDisconnectedEvent = new AutoResetEvent(false);
        final String[] aDisconnectedResponseReceiver = new String[1];
        aDuplexInputChannel.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aDisconnectedResponseReceiver[0] = y.getResponseReceiverId();
                aResponseReceiverDisconnectedEvent.set();
            }
        });
        
        final AutoResetEvent aMessageReceivedEvent = new AutoResetEvent(false);
        final String[] aReceivedMessage = new String[1];
        aDuplexInputChannel.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                aReceivedMessage[0] = (String)y.getMessage();
                aMessageReceivedEvent.set();
            }
        });
        
        try
        {
            // Start listening.
            aDuplexInputChannel.startListening();
            assertTrue(aDuplexInputChannel.isListening());

            // Connect duplex output channel 1
            aDuplexOutputChannel1.openConnection();

            // Wait until connected.
            aResponseReceiverConnectedEvent.waitOne();
            assertEquals(aDuplexOutputChannel1.getResponseReceiverId(), aConnectedResponseReceiver[0]);
            assertTrue(aDuplexOutputChannel1.isConnected());

            // Connect duplex output channel 2
            aDuplexOutputChannel2.openConnection();

            // Wait until connected.
            aResponseReceiverConnectedEvent.waitOne();
            assertEquals(aDuplexOutputChannel2.getResponseReceiverId(), aConnectedResponseReceiver[0]);
            assertTrue(aDuplexOutputChannel2.isConnected());


            // Disconnect duplex output channel 1
            aDuplexOutputChannel1.closeConnection();

            // Wait until disconnected
            aResponseReceiverDisconnectedEvent.waitOne();
            Thread.sleep(100); // maybe other unwanted disconnection - give them some time.
            assertFalse(aDuplexOutputChannel1.isConnected());
            assertTrue(aDuplexOutputChannel2.isConnected());
            assertEquals(aDuplexOutputChannel1.getResponseReceiverId(), aDisconnectedResponseReceiver[0]);

            // The second duplex output channel must still work.
            aDuplexOutputChannel2.sendMessage("Message");

            aMessageReceivedEvent.waitOne();
            assertEquals("Message", aReceivedMessage[0]);
        }
        finally
        {
            aDuplexOutputChannel1.closeConnection();
            aDuplexOutputChannel2.closeConnection();
            aDuplexInputChannel.stopListening();
        }
    }
    
    @Test
    public void Duplex_12_CloseFromConnectionOpened()
        throws Exception
    {
        final IDuplexInputChannel aDuplexInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        final IDuplexOutputChannel aDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        final boolean[] isOpenedFlag = {false};
        aDuplexOutputChannel.connectionOpened().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object t1, DuplexChannelEventArgs t2)
            {
                isOpenedFlag[0] = aDuplexOutputChannel.isConnected();

                // Try to close the connection from this "open" event handler.
                aDuplexOutputChannel.closeConnection();
            }
        });

        final boolean[] isClosedFlag = {false};
        final AutoResetEvent aConnectionClosedEvent = new AutoResetEvent(false);
        aDuplexOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object t1, DuplexChannelEventArgs t2)
            {
                isClosedFlag[0] = aDuplexOutputChannel.isConnected() == false;
                aConnectionClosedEvent.set();
            }
        });
        
        try
        {
            aDuplexInputChannel.startListening();

            // Open connection - the event will try to close the connection.
            aDuplexOutputChannel.openConnection();

            aConnectionClosedEvent.waitOne();

            assertTrue(isOpenedFlag[0]);
            assertTrue(isClosedFlag[0]);
        }
        finally
        {
            aDuplexInputChannel.stopListening();
            aDuplexOutputChannel.closeConnection();
        }

    }
    
    @Test
    public void Duplex_13_DisconnectFromResponseReceiverConnected()
        throws Exception
    {
        final IDuplexInputChannel aDuplexInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        final IDuplexOutputChannel aDuplexOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        final String[] aConnectedResponseReceiver = {""};
        aDuplexInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aConnectedResponseReceiver[0] = y.getResponseReceiverId();

                try
                {
                    aDuplexInputChannel.disconnectResponseReceiver(aConnectedResponseReceiver[0]);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Disconnecting the response receiver failed.", err);
                }
            }
        });
        
        final boolean[] isDisconnectedFlag = {false};
        final AutoResetEvent aConnectionClosedEvent = new AutoResetEvent(false);
        aDuplexOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object t1, DuplexChannelEventArgs t2)
            {
                isDisconnectedFlag[0] = aDuplexOutputChannel.isConnected() == false;
                aConnectionClosedEvent.set();
            }
        });

        try
        {
            aDuplexInputChannel.startListening();

            // Open connection - the event will try to close the connection.
            aDuplexOutputChannel.openConnection();

            aConnectionClosedEvent.waitOne();

            assertEquals(aDuplexOutputChannel.getResponseReceiverId(), aConnectedResponseReceiver[0]);
            assertTrue(isDisconnectedFlag[0]);
        }
        finally
        {
            aDuplexInputChannel.stopListening();
            aDuplexOutputChannel.closeConnection();
        }
    }
    
    
    protected void sendMessageReceiveResponse(final String channelId,
            final String message, final String resonseMessage,
            final int numberOfClients, final int numberOfTimes)
            throws Exception
    {
        TDuplexClient[] aClients = new TDuplexClient[numberOfClients];
        for (int i = 0; i < numberOfClients; ++i)
        {
            aClients[i] = new TDuplexClient(myMessagingSystemFactory, channelId, resonseMessage, numberOfTimes);
        }

        TDuplexService aService = new TDuplexService(myMessagingSystemFactory, channelId, message, numberOfTimes * numberOfClients, resonseMessage);

        
        try
        {
            // Input channel starts listening
            aService.InputChannel.startListening();

            // Clients open connection in parallel.
            for (TDuplexClient aClient : aClients)
            {
                final TDuplexClient aC = aClient;
                ThreadPool.queueUserWorkItem(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            aC.openConnection();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error("Opening connection failed.", err);
                        }
                    }
                });
                
                Thread.sleep(2);
            }

            // Wait until connections are open.
            for (TDuplexClient aClient : aClients)
            {
                // Wait until the connection is open.
                aClient.ConnectionOpenEvent.waitOne();
                //assertTrue(aClient.ConnectionOpenEvent.waitOne(10000));
            }

            // Clients send messages in parallel.
            for (TDuplexClient aClient : aClients)
            {
                final TDuplexClient aC = aClient;
                ThreadPool.queueUserWorkItem(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //EneterTrace.Info("Client idx = " + aIdx);

                        for (int j = 0; j < numberOfTimes; ++j)
                        {
                            // Send messages.
                            try
                            {
                                aC.OutputChannel.sendMessage(message);
                            }
                            catch (Exception err)
                            {
                                EneterTrace.error("Sending of a message failed.", err);
                            }
                        }
                    }
                });

                Thread.sleep(2);
            }

            // Wait until all messages are processed.
            for (TDuplexClient aClient : aClients)
            {
                //assertTrue(aClient.ResponsesReceivedEvent.waitOne(timeOutForMessageProcessing));
                aClient.ResponsesReceivedEvent.waitOne();
            }

            EneterTrace.info("Waiting for processing of messages on '" + channelId + "' completed.");
        }
        finally
        {
            try
            {
                for (TDuplexClient aClient : aClients)
                {
                    aClient.OutputChannel.closeConnection();
                }
            }
            finally
            {
                aService.InputChannel.stopListening();
            }
        }

        for (TDuplexClient aClient : aClients)
        {
            assertEquals("There are failed response messages.", 0, aClient.NumberOfFailedResponses);
            assertEquals("Number of received responses differs from number of sent responses.", numberOfTimes, aClient.NumberOfReceivedResponses);
        }

        assertEquals("There are failed messages.", 0, aService.NumberOfFailedMessages);
        assertEquals("Number of sent messages differs from number of received.", numberOfTimes * numberOfClients, aService.NumberOfReceivedMessages);
    }
    
    
    protected static String getString(int length)
    {
        StringBuilder aStringBuilder = new StringBuilder();

        for (int i = 0; i < length; ++i)
        {
            char ch = (char)((int)(Math.random() * 26 + 97)); 
            aStringBuilder.append(ch);
        }

        return aStringBuilder.toString();
    }

    protected IMessagingSystemFactory myMessagingSystemFactory;
    protected String myChannelId;
    
    private static String myMessage_10MB = getString(10000000);
}

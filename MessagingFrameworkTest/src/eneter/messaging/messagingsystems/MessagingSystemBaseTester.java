package eneter.messaging.messagingsystems;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
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
        sendMessageReceiveResponse(myChannelId, "Message", "Response", 1, 5000);
    }
    
    @Test
    public void A06_SendMessageReceiveResponse500()
            throws Exception
    {
        sendMessageReceiveResponse(myChannelId, "Message", "Respones", 500, 5000);
    }
    
    @Test(expected = IllegalStateException.class)
    public void A07_StopListening()
        throws Exception
    {
        IOutputChannel anOutputChannel = myMessagingSystemFactory.createOutputChannel(myChannelId);
        IInputChannel anInputChannel = myMessagingSystemFactory.createInputChannel(myChannelId);

        anInputChannel.messageReceived().subscribe(new EventHandler<ChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object t1, ChannelMessageEventArgs t2)
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
    
    
    @Test
    public void A08_MultithreadSendMessage()
        throws Exception
    {
        final IOutputChannel anOutputChannel = myMessagingSystemFactory.createOutputChannel(myChannelId);
        final IInputChannel anInputChannel = myMessagingSystemFactory.createInputChannel(myChannelId);

        final ManualResetEvent aMessagesSentEvent = new ManualResetEvent(false);

        // Observe the input channel
        final ArrayList<String> aReceivedMessages = new ArrayList<String>();
        anInputChannel.messageReceived().subscribe(new EventHandler<ChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, ChannelMessageEventArgs y)
            {
                aReceivedMessages.add((String)y.getMessage());

                if (aReceivedMessages.size() == 500)
                {
                    aMessagesSentEvent.set();
                }
            }
        });
        
        
        // Create 10 competing threads
        final ArrayList<Thread> aThreads = new ArrayList<Thread>();
        
        for (int t = 0; t < 10; ++t)
        {
            Thread aThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                 // Send 50 messages
                    for (int i = 0; i < 50; ++i)
                    {
                        try
                        {
                            Thread.sleep(1); // To mix the order of threads. (othewise it would go thread by thread)
                            long aThreadId = Thread.currentThread().getId();
                            anOutputChannel.sendMessage(String.valueOf(aThreadId));
                        }
                        catch (Exception err)
                        {
                        }
                    }
                }
            }); 
                    
            aThreads.add(aThread);
        }

        try
        {
            anInputChannel.startListening();

            // Start sending from threads
            for (Thread t : aThreads)
            {
                t.start();
            }

            // Wait until all messages are received.
            assertTrue(aMessagesSentEvent.waitOne(30000));
        }
        finally
        {
            anInputChannel.stopListening();
        }

        // Check
        assertEquals(500, aReceivedMessages.size());
    }
    
    @Test
    public void A09_OpenCloseConnection()
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

            aConnectionOpenedEvent.waitOne();

            assertEquals(anOutputChannel.getChannelId(), aConnectionOpenedEventArgs[0].getChannelId());
            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectionOpenedEventArgs[0].getResponseReceiverId());

            aResponseReceiverConnectedEvent.waitOne();

            assertEquals(anOutputChannel.getResponseReceiverId(), aConnectedReceiver[0]);


            anOutputChannel.closeConnection();

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
    public void A10_OpenConnectionIfDuplexInputChannelNotStarted() throws Exception
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
    public void A11_DuplexInputChannelSuddenlyStopped() throws Exception
    {
        IDuplexInputChannel anInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        IDuplexOutputChannel anOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

        boolean isSomeException = false;

        try
        {
            // Duplex input channel starts to listen.
            anInputChannel.startListening();

            // Duplex output channel connects.
            anOutputChannel.openConnection();
            assertTrue(anOutputChannel.isConnected());

            Thread.sleep(100);


            // Duplex input channel stops to listen.
            anInputChannel.stopListening();

            assertFalse(anInputChannel.isListening());

            //Thread.Sleep(3000);

            // Try to send a message via the duplex output channel.
            anOutputChannel.sendMessage("Message");
        }
        catch (Exception err)
        {
            // Because the duplex input channel is not listening the sending must
            // fail with an exception. The type of the exception depends from the type of messaging system.
            isSomeException = true;
        }
        finally
        {
            anOutputChannel.closeConnection();
        }

        assertTrue(isSomeException);
    }
    
    @Test
    public void A12_DuplexInputChannelDisconnectsResponseReceiver()
        throws Exception
    {
        final AutoResetEvent aResponseReceiverConnectedEvent = new AutoResetEvent(false);
        final AutoResetEvent aConnectionClosedEvent = new AutoResetEvent(false);

        final boolean[] aResponseReceiverConnectedFlag = new boolean[1];
        //bool aResponseReceiverDisconnectedFlag = false;

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
                //aResponseReceiverDisconnectedFlag = true;
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
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }

        assertTrue(aResponseReceiverConnectedFlag[0]);
        assertTrue(aConnectionClosedReceivedInOutputChannelFlag[0]);

        assertFalse(aResponseMessageReceivedFlag[0]);
        
        // Note: When the duplex input channel disconnected the duplex output channel, the notification, that
        //       the duplex output channel was disconnected does not have to be invoked.
        //Assert.IsFalse(aResponseReceiverDisconnectedFlag);
    }
    
    @Test
    public void A13_DuplexOutputChannelClosesConnection()
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
    public void A14_DuplexOutputChannelDisconnected_OpenFromCloseHandler()
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
    public void A15_DuplexOutputChannelConnected_CloseFromOpenHandler()
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
    public void A16_DuplexOutputChannelConnectionOpened_DisconnectFromOpenHandler()
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
    
    
    
    
    protected void sendMessageViaOutputChannel(final String channelId, final Object message, final int numberOfTimes, int timeOutForMessageProcessing)
            throws Exception
    {
        IOutputChannel anOutputChannel = myMessagingSystemFactory.createOutputChannel(channelId);
        IInputChannel anInputChannel = myMessagingSystemFactory.createInputChannel(channelId);

        final ManualResetEvent aMessagesSentEvent = new ManualResetEvent(false);

        final int[] aNumberOfReceivedMessages = new int[1];
        final int[] aNumberOfFailures = new int[1];
        
        EventHandler<ChannelMessageEventArgs> aMessageReceivedEventHandler = new EventHandler<ChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, ChannelMessageEventArgs y)
            {
                // Some messaging system can have a parallel access therefore we must ensure
                // that results are put to the list synchronously.
                synchronized(myLock)
                {
                    ++aNumberOfReceivedMessages[0];

                    if (!channelId.equals(y.getChannelId()) || !message.equals(y.getMessage()))
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

            EneterTrace.info("Send messages to '" + channelId + "' completed - waiting while they are processed.");

            // Wait until all messages are processed.
            assertTrue(aMessagesSentEvent.waitOne(timeOutForMessageProcessing));
            //assertTrue(aMessagesSentEvent.waitOne(60000));

            EneterTrace.info("Waiting for processing of messages on '" + channelId + "' completed.");
        }
        finally
        {
            anInputChannel.stopListening();
        }

        assertEquals(0, aNumberOfFailures[0]);
        assertEquals(numberOfTimes, aNumberOfReceivedMessages[0]);
    }
    
    
    protected void sendMessageReceiveResponse(final String channelId, final Object message, final Object resonseMessage, final int numberOfTimes, int timeOutForMessageProcessing)
            throws Exception
    {
        IDuplexOutputChannel anOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(channelId);
        final IDuplexInputChannel anInputChannel = myMessagingSystemFactory.createDuplexInputChannel(channelId);

        final AutoResetEvent aMessagesSentEvent = new AutoResetEvent(false);

        final int[] aNumberOfReceivedMessages = new int[1];
        final int[] aNumberOfFailedMessages = new int[1];
        
        EventHandler<DuplexChannelMessageEventArgs> aMessageReceivedHandler = new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                // Some messaging system can have a parallel access therefore we must ensure
                // that results are put to the list synchronously.
                synchronized (amyMessageReceiverLock)
                {
                    ++aNumberOfReceivedMessages[0];

                    if (!channelId.equals(y.getChannelId()) || !message.equals(y.getMessage()))
                    {
                        ++aNumberOfFailedMessages[0];
                    }
                    else
                    {
                        // everything is ok -> send the response
                        try
                        {
							anInputChannel.sendResponseMessage(y.getResponseReceiverId(), resonseMessage);
						}
                        catch (Exception err)
                        {
							EneterTrace.error("Sending response message failed.", err);
						}
                    }
                }
            }
            
            private Object amyMessageReceiverLock = new Object();
        };
        
        anInputChannel.messageReceived().subscribe(aMessageReceivedHandler);
        
        
        final int[] aNumberOfReceivedResponses = new int[1];
        final int[] aNumberOfFailedResponses = new int[1];
        
        EventHandler<DuplexChannelMessageEventArgs> aResponseReceivedHandler = new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (amyResponseReceiverLock)
                {
                    ++aNumberOfReceivedResponses[0];
                    if (!resonseMessage.equals(y.getMessage()))
                    {
                        ++aNumberOfFailedResponses[0];
                    }

                    //EneterTrace.info("Responses: " + aNumberOfReceivedResponses[0]);
                    
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
            
            // give some time to be sure the listening is fully activated.
            Thread.sleep(100);
            
            // Output channel connects in order to be able to receive response messages.
            anOutputChannel.openConnection();
            
            // give some time to be sure the connection is fully activated.
            Thread.sleep(100);

            // Send messages
            for (int i = 0; i < numberOfTimes; ++i)
            {
                anOutputChannel.sendMessage(message);
            }

            EneterTrace.info("Send messages to '" + channelId + "' completed - waiting while they are processed.");

            // Wait until all messages are processed.
            assertTrue(aMessagesSentEvent.waitOne(timeOutForMessageProcessing));
            //assertTrue(aMessagesSentEvent.waitOne(60000));

            EneterTrace.info("Waiting for processing of messages on '" + channelId + "' completed.");
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

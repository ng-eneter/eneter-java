package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;

import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.AutoResetEvent;

public abstract class BufferedMessagingBaseTester
{
    @Test
    public void A01_SimpleRequestResponse() throws Exception
    {
        final IDuplexOutputChannel aDuplexOutputChannel = MessagingSystem.createDuplexOutputChannel(ChannelId);
        final IDuplexInputChannel aDuplexInputChannel = MessagingSystem.createDuplexInputChannel(ChannelId);


        // Received messages.
        final ArrayList<Integer> aReceivedMessages = new ArrayList<Integer>();
        aDuplexInputChannel.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (aReceivedMessages)
                {
                    String aReceivedMessage = (String)y.getMessage();

                    int k = Integer.parseInt(aReceivedMessage);
                    aReceivedMessages.add(k);
                    k += 1000;

                    try
                    {
						aDuplexInputChannel.sendResponseMessage(y.getResponseReceiverId(), Integer.toString(k));
					}
                    catch (Exception err)
                    {
						EneterTrace.error("Sending response message failed.", err);
					}
                }
            }
        });
        
        // Received response messages.
        final ArrayList<Integer> aReceivedResponseMessages = new ArrayList<Integer>();
        final AutoResetEvent anAllMessagesProcessedEvent = new AutoResetEvent(false);
        aDuplexOutputChannel.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (aReceivedResponseMessages)
                {
                    String aReceivedMessage = (String)y.getMessage();

                    int k = Integer.parseInt(aReceivedMessage);
                    aReceivedResponseMessages.add(k);

                    if (k == 1019)
                    {
                        anAllMessagesProcessedEvent.set();
                    }
                }
            }
        });
        
        try
        {
            aDuplexInputChannel.startListening();
            aDuplexOutputChannel.openConnection();

            for (int i = 0; i < 20; ++i)
            {
                aDuplexOutputChannel.sendMessage(Integer.toString(i));
            }

            // Wait untill all messages are processed.
            //anAllMessagesProcessedEvent.waitOne();
            assertTrue(anAllMessagesProcessedEvent.waitOne(60000));

        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }

        Collections.sort(aReceivedMessages);
        assertEquals(20, aReceivedMessages.size());
        for (int i = 0; i < 20; ++i)
        {
            assertEquals(i, (int)aReceivedMessages.get(i));
        }

        Collections.sort(aReceivedResponseMessages);
        assertEquals(20, aReceivedResponseMessages.size());
        for (int i = 0; i < 20; ++i)
        {
            assertEquals(i + 1000, (int)aReceivedResponseMessages.get(i));
        }


    }
    
    @Test
    public void A02_IndependentStartupOrder() throws Exception
    {
        final IDuplexOutputChannel aDuplexOutputChannel = MessagingSystem.createDuplexOutputChannel(ChannelId);
        final IDuplexInputChannel aDuplexInputChannel = MessagingSystem.createDuplexInputChannel(ChannelId);


        // Received messages.
        final ArrayList<Integer> aReceivedMessages = new ArrayList<Integer>();
        aDuplexInputChannel.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (aReceivedMessages)
                {
                    String aReceivedMessage = (String)y.getMessage();

                    int k = Integer.parseInt(aReceivedMessage);
                    aReceivedMessages.add(k);
                    k += 1000;

                    try
                    {
						aDuplexInputChannel.sendResponseMessage(y.getResponseReceiverId(), Integer.toString(k));
					}
                    catch (Exception err)
                    {
                    	EneterTrace.error("Sending response message failed.", err);
					}
                }
            }
        });

        // Received response messages.
        final ArrayList<Integer> aReceivedResponseMessages = new ArrayList<Integer>();
        final AutoResetEvent anAllMessagesProcessedEvent = new AutoResetEvent(false);
        aDuplexOutputChannel.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (aReceivedResponseMessages)
                {
                    String aReceivedMessage = (String)y.getMessage();

                    int k = Integer.parseInt(aReceivedMessage);
                    aReceivedResponseMessages.add(k);

                    if (k == 1019)
                    {
                        anAllMessagesProcessedEvent.set();
                    }
                }
            }
        });


        try
        {
            aDuplexOutputChannel.openConnection();

            Thread.sleep(500);

            aDuplexInputChannel.startListening();
            

            for (int i = 0; i < 20; ++i)
            {
                aDuplexOutputChannel.sendMessage(Integer.toString(i));
            }

            // Wait untill all messages are processed.
            //anAllMessagesProcessedEvent.waitOne();
            assertTrue(anAllMessagesProcessedEvent.waitOne(60000));

        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }

        Collections.sort(aReceivedMessages);
        assertEquals(20, aReceivedMessages.size());
        for (int i = 0; i < 20; ++i)
        {
            assertEquals(i, (int)aReceivedMessages.get(i));
        }

        Collections.sort(aReceivedResponseMessages);
        assertEquals(20, aReceivedResponseMessages.size());
        for (int i = 0; i < 20; ++i)
        {
            assertEquals(i + 1000, (int)aReceivedResponseMessages.get(i));
        }


    }
    

    @Test
    public void A04_SendMessagesOffline() throws Exception
    {
        final IDuplexOutputChannel aDuplexOutputChannel = MessagingSystem.createDuplexOutputChannel(ChannelId);
        final IDuplexInputChannel aDuplexInputChannel = MessagingSystem.createDuplexInputChannel(ChannelId);


        // Received messages.
        final ArrayList<Integer> aReceivedMessages = new ArrayList<Integer>();
        aDuplexInputChannel.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (aReceivedMessages)
                {
                    String aReceivedMessage = (String)y.getMessage();

                    int k = Integer.parseInt(aReceivedMessage);
                    aReceivedMessages.add(k);
                    k += 1000;

                    try
                    {
						aDuplexInputChannel.sendResponseMessage(y.getResponseReceiverId(), Integer.toString(k));
					}
                    catch (Exception err)
                    {
                    	EneterTrace.error("Sending response message failed.", err);
					}
                }
            }
        });
        
        // Received response messages.
        final ArrayList<Integer> aReceivedResponseMessages = new ArrayList<Integer>();
        final AutoResetEvent anAllMessagesProcessedEvent = new AutoResetEvent(false);
        aDuplexOutputChannel.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (aReceivedResponseMessages)
                {
                    String aReceivedMessage = (String)y.getMessage();

                    int k = Integer.parseInt(aReceivedMessage);
                    aReceivedResponseMessages.add(k);

                    if (k == 1019)
                    {
                        anAllMessagesProcessedEvent.set();
                    }
                }
            }
        });

        try
        {
            aDuplexOutputChannel.openConnection();

            for (int i = 0; i < 20; ++i)
            {
                aDuplexOutputChannel.sendMessage(Integer.toString(i));
            }

            Thread.sleep(500);

            aDuplexInputChannel.startListening();

            // Wait untill all messages are processed.
            //anAllMessagesProcessedEvent.waitOne();
            assertTrue(anAllMessagesProcessedEvent.waitOne(60000));

        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }

        Collections.sort(aReceivedMessages);
        assertEquals(20, aReceivedMessages.size());
        for (int i = 0; i < 20; ++i)
        {
            assertEquals(i, (int)aReceivedMessages.get(i));
        }

        Collections.sort(aReceivedResponseMessages);
        assertEquals(20, aReceivedResponseMessages.size());
        for (int i = 0; i < 20; ++i)
        {
            assertEquals(i + 1000, (int)aReceivedResponseMessages.get(i));
        }
    }

    @Test
    public void A05_SendResponsesOffline() throws Exception
    {
        final IDuplexOutputChannel aDuplexOutputChannel = MessagingSystem.createDuplexOutputChannel(ChannelId, "MyResponseReceiverId");
        final IDuplexInputChannel aDuplexInputChannel = MessagingSystem.createDuplexInputChannel(ChannelId);


        // Received response messages.
        final ArrayList<Integer> aReceivedResponseMessages = new ArrayList<Integer>();
        final AutoResetEvent anAllMessagesProcessedEvent = new AutoResetEvent(false);
        aDuplexOutputChannel.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (aReceivedResponseMessages)
                {
                    String aReceivedMessage = (String)y.getMessage();

                    int k = Integer.parseInt(aReceivedMessage);
                    aReceivedResponseMessages.add(k);

                    if (aReceivedResponseMessages.size() == 20)
                    {
                        anAllMessagesProcessedEvent.set();
                    }
                }
            }
        });


        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        try
        {
            aDuplexInputChannel.startListening();

            // Send messages to the response receiver, that is not connected.
            for (int i = 0; i < 20; ++i)
            {
                aDuplexInputChannel.sendResponseMessage("MyResponseReceiverId", Integer.toString(i));
            }

            Thread.sleep(500);

            aDuplexOutputChannel.openConnection();

            // Wait untill all messages are processed.
            //anAllMessagesProcessedEvent.waitOne();
            assertTrue(anAllMessagesProcessedEvent.waitOne(60000));

        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }

        Collections.sort(aReceivedResponseMessages);
        assertEquals(20, aReceivedResponseMessages.size());
        for (int i = 0; i < 20; ++i)
        {
            assertEquals(i, (int)aReceivedResponseMessages.get(i));
        }
    }
    
    @Test
    public void A06_TimeoutedResponseReceiver() throws Exception
    {
        // Duplex output channel without queue - it will not try to reconnect.
        final IDuplexOutputChannel aDuplexOutputChannel = UnderlyingMessaging.createDuplexOutputChannel(ChannelId);
        final IDuplexInputChannel aDuplexInputChannel = MessagingSystem.createDuplexInputChannel(ChannelId);

        final AutoResetEvent aConnectionClosedEvent = new AutoResetEvent(false);
        aDuplexOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelEventArgs y)
            {
                aConnectionClosedEvent.set();
            }
        });

        final String[] aDisconnectedResponseReceiverId = {""};
        final AutoResetEvent aResponseReceiverDisconnectedEvent = new AutoResetEvent(false);
        aDuplexInputChannel.responseReceiverDisconnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                aDisconnectedResponseReceiverId[0] = y.getResponseReceiverId();
                aResponseReceiverDisconnectedEvent.set();
            }
        });
        
        try
        {
            aDuplexInputChannel.startListening();
            aDuplexOutputChannel.openConnection();

            // Disconnect the output channel.
            // The input channel should disconnect the client after max offline time.
            aDuplexOutputChannel.closeConnection();

            assertTrue(aConnectionClosedEvent.waitOne(30000));
            assertTrue(aResponseReceiverDisconnectedEvent.waitOne(30000));
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }

    }
    
    @Test
    public void A07_ResponseReceiverReconnects_AfterDisconnect() throws Exception
    {
        // Duplex output channel without queue - it will not try to reconnect.
        final IDuplexOutputChannel aDuplexOutputChannel = MessagingSystem.createDuplexOutputChannel(ChannelId);
        final IDuplexInputChannel aDuplexInputChannel = MessagingSystem.createDuplexInputChannel(ChannelId);

        final AutoResetEvent aConnectionsCompletedEvent = new AutoResetEvent(false);
        final ArrayList<String> anOpenConnections = new ArrayList<String>();
        aDuplexInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                synchronized (anOpenConnections)
                {
                    anOpenConnections.add(y.getResponseReceiverId());

                    aConnectionsCompletedEvent.set();
                }
            }
        });

        try
        {
            aDuplexInputChannel.startListening();
            aDuplexOutputChannel.openConnection();

            // Wait until the connection is open.
            aConnectionsCompletedEvent.waitOne();
            
            // Disconnect the response receiver.
            aDuplexInputChannel.disconnectResponseReceiver(aDuplexOutputChannel.getResponseReceiverId());

            // The duplex output channel will try to connect again, therefore wait until connected.
            aConnectionsCompletedEvent.waitOne();
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }


        assertEquals(2, anOpenConnections.size());

        // Both connections should be same.
        assertEquals(aDuplexOutputChannel.getResponseReceiverId(), anOpenConnections.get(0));
        assertEquals(aDuplexOutputChannel.getResponseReceiverId(), anOpenConnections.get(1));
    }
    
    @Test
    public void A08_ResponseReceiverReconnects_AfterStopListening() throws Exception
    {
        // Duplex output channel without queue - it will not try to reconnect.
        final IDuplexOutputChannel aDuplexOutputChannel = MessagingSystem.createDuplexOutputChannel(ChannelId);
        final IDuplexInputChannel aDuplexInputChannel = MessagingSystem.createDuplexInputChannel(ChannelId);

        final AutoResetEvent aConnectionsCompletedEvent = new AutoResetEvent(false);
        final ArrayList<String> anOpenConnections = new ArrayList<String>();
        aDuplexInputChannel.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object x, ResponseReceiverEventArgs y)
            {
                synchronized (anOpenConnections)
                {
                    anOpenConnections.add(y.getResponseReceiverId());
                    aConnectionsCompletedEvent.set();
                }
            }
        });

        try
        {
            //EneterTrace.setDetailLevel(EDetailLevel.Debug);
            
            aDuplexInputChannel.startListening();
            aDuplexOutputChannel.openConnection();

            // Wait until the client is connected.
            aConnectionsCompletedEvent.waitOne();
            
            // Stop listenig.
            aDuplexInputChannel.stopListening();

            // Give some time to stop.
            Thread.sleep(300);

            // Start listening again.
            aDuplexInputChannel.startListening();

            // The duplex output channel will try to connect again, therefore wait until connected.
            aConnectionsCompletedEvent.waitOne();

            assertTrue(aDuplexOutputChannel.isConnected());
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }


        assertEquals(2, anOpenConnections.size());

        // Both connections should be same.
        assertEquals(aDuplexOutputChannel.getResponseReceiverId(), anOpenConnections.get(0));
        assertEquals(aDuplexOutputChannel.getResponseReceiverId(), anOpenConnections.get(1));
    }
    
    @Test
    public void A09_RequestResponse_100_ConstantlyInterrupted() throws Exception
    {
        final IDuplexOutputChannel aDuplexOutputChannel = MessagingSystem.createDuplexOutputChannel(ChannelId);
        final IDuplexInputChannel aDuplexInputChannel = MessagingSystem.createDuplexInputChannel(ChannelId);

        Field aMember = aDuplexInputChannel.getClass().getDeclaredField("myUnderlyingInputChannel");
        aMember.setAccessible(true);
        final IDuplexInputChannel anUnderlyingDuplexInputChannel = (IDuplexInputChannel)aMember.get(aDuplexInputChannel);


        final AutoResetEvent anAllMessagesProcessedEvent = new AutoResetEvent(false);

        // Received messages.
        final ArrayList<Integer> aReceivedMessages = new ArrayList<Integer>();
        aDuplexInputChannel.messageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (aReceivedMessages)
                {
                    String aReceivedMessage = (String)y.getMessage();

                    EneterTrace.info("Received message: " + aReceivedMessage);

                    int k = Integer.parseInt(aReceivedMessage);
                    aReceivedMessages.add(k);
                    k += 1000;

                    EneterTrace.info("Sent response message: " + Integer.toString(k));
                    try 
                    {
						aDuplexInputChannel.sendResponseMessage(y.getResponseReceiverId(), Integer.toString(k));
					}
                    catch (Exception err)
                    {
                    	EneterTrace.error("Sending response message failed.", err);
					}
                }
            }
        });
        
        aDuplexOutputChannel.connectionClosed().subscribe(new EventHandler<DuplexChannelEventArgs>()
        {
            @Override
            public void onEvent(Object t1, DuplexChannelEventArgs t2)
            {
                EneterTrace.info("ConnectionClosed invoked in duplex output channel");

                // The buffered duplex output channel exceeded the max offline time.
                anAllMessagesProcessedEvent.set();
            }
        });
        

        // Received response messages.
        final ArrayList<Integer> aReceivedResponseMessages = new ArrayList<Integer>();
        aDuplexOutputChannel.responseMessageReceived().subscribe(new EventHandler<DuplexChannelMessageEventArgs>()
        {
            @Override
            public void onEvent(Object x, DuplexChannelMessageEventArgs y)
            {
                synchronized (aReceivedResponseMessages)
                {
                    String aReceivedMessage = (String)y.getMessage();

                    EneterTrace.info("Received response message: " + aReceivedMessage);

                    int k = Integer.parseInt(aReceivedMessage);
                    aReceivedResponseMessages.add(k);

                    if (aReceivedResponseMessages.size() == 100)
                    {
                        anAllMessagesProcessedEvent.set();
                    }
                }
            }
        });
        
        try
        {
            final boolean[] aTestFinishedFlag = {false};

            aDuplexInputChannel.startListening();
            aDuplexOutputChannel.openConnection();


            Thread anInteruptingThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        for (int i = 0; i < 100 && !aTestFinishedFlag[0]; ++i)
                        {
                            anUnderlyingDuplexInputChannel.disconnectResponseReceiver(aDuplexOutputChannel.getResponseReceiverId());
                            Thread.sleep(ConnectionInterruptionFrequency);
                        }
                    }
                    catch (Exception err)
                    {
                    }
                }
            });
                    
            // Start constant disconnecting.
            anInteruptingThread.start();

            for (int i = 0; i < 100; ++i)
            {
                aDuplexOutputChannel.sendMessage(Integer.toString(i));
            }

            // Note: This test can fail.
            //       The problem is, if the connection was broken
            //       at the moment when the client successfully sent the message
            //       but the message did not reach the service yet.
            
            // Wait until all messages are processed.
            //anAllMessagesProcessedEvent.WaitOne();
            assertTrue(anAllMessagesProcessedEvent.waitOne(15000));

            aTestFinishedFlag[0] = true;
            anInteruptingThread.join();
        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }

        Collections.sort(aReceivedMessages);
        assertEquals(100, aReceivedMessages.size());
        for (int i = 0; i < 100; ++i)
        {
            assertEquals(i, (int)aReceivedMessages.get(i));
        }

        Collections.sort(aReceivedResponseMessages);
        assertEquals(100, aReceivedResponseMessages.size());
        for (int i = 0; i < 100; ++i)
        {
            assertEquals(i + 1000, (int)aReceivedResponseMessages.get(i));
        }
    }
    
    protected String ChannelId;
    protected IMessagingSystemFactory UnderlyingMessaging;
    protected IMessagingSystemFactory MessagingSystem;
    protected int ConnectionInterruptionFrequency;
}

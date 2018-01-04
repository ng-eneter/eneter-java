package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import static org.junit.Assert.*;

import helper.PerformanceTimer;
import helper.RandomDataGenerator;

import java.lang.reflect.*;
import java.util.*;

import org.hamcrest.core.IsEqual;
import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.IFunction1;
import eneter.net.system.linq.internal.EnumerableExt;
import eneter.net.system.threading.internal.AutoResetEvent;



public abstract class BufferedMessagingBaseTester
{
    @Test
    public void A01_Send1() throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 1, 1, 1000, 500);
    }
    
    @Test
    public void A02_Send500() throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 1, 500, 1000, 1000);
    }
    
    @Test
    public void A03_Send1_10M() throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myMessage_10MB, myMessage_10MB, 1, 1, 1000, 5000);
    }
    
    @Test
    public void A04_Send50000() throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 1, 50000, 500, 20000);
    }
    
    @Test
    public void A05_Send50_10Prallel() throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 10, 50, 1000, 10000);
    }
    
    @Test
    public void A06_IndependentStartupOrder() throws Exception
    {
        sendOfflineMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 1, 1, 3000, 3000);
    }
    
    @Test
    public void A07_IndependentStartupOrder50_10Parallel() throws Exception
    {
        sendOfflineMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 10, 50, 3000, 3000);
    }
    
    @Test
    public void A08_SendMessagesOffline10() throws Exception
    {
        sendOfflineMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 1, 10, 3000, 3000);
    }
    
    @Test
    public void A09_SendResponsesOffline() throws Exception
    {
        sendOfflineResponse(ChannelId, myResponseMessage, 1, 1, 1000, 1000);
    }
    
    @Test
    public void A10_SendResponsesOffline10() throws Exception
    {
        sendOfflineResponse(ChannelId, myResponseMessage, 1, 10, 1000, 1000);
    }
    
    @Test
    public void A11_SendResponsesOffline10_10Parallel() throws Exception
    {
        sendOfflineResponse(ChannelId, myResponseMessage, 10, 10, 1000, 1000);
    }
    
    @Test
    public void A12_SendBroadcastResponse_50_10Clients() throws Exception
    {
        sendBroadcastResponseMessage(ChannelId, myResponseMessage, 10, 50, 1000, 2000);sendOfflineBroadcastResponseMessage(ChannelId, myResponseMessage, 10, 50, 1000, 2000);
    }
    
    @Test
    public void A12_SendOfflineBroadcastResponse_50_10Clients() throws Exception
    {
        sendOfflineBroadcastResponseMessage(ChannelId, myResponseMessage, 10, 50, 1000, 2000);
    }
    
    @Test
    public void A13_TimeoutedResponseReceiver() throws Exception
    {
        ClientMock aClient = new ClientMock(MessagingSystem, ChannelId);
        ServiceMock aService = new ServiceMock(MessagingSystem, ChannelId);

        try
        {
            aService.getInputChannel().startListening();

            // Open the connection.
            aClient.getOutputChannel().openConnection();
            assertTrue(aClient.getOutputChannel().isConnected());

            // handling open connection on the client side.
            EneterTrace.info("1");
            aClient.waitUntilConnectionOpenIsNotified(2000);
            assertEquals(aClient.getOutputChannel().getChannelId(), aClient.getNotifiedOpenConnection().getChannelId());
            assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aClient.getNotifiedOpenConnection().getResponseReceiverId());

            // handling open connection on the service side.
            EneterTrace.info("2");
            aService.waitUntilResponseReceiversConnectNotified(1, 1000);
            assertEquals(1, aService.getConnectedResponseReceivers().size());
            assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aService.getConnectedResponseReceivers().get(0).getResponseReceiverId());

            aClient.getOutputChannel().closeConnection();
            assertFalse(aClient.getOutputChannel().isConnected());

            // Service will disconnect the response receiver when the offline timout is exceeded.
            EneterTrace.info("3");
            aService.waitUntilAllResponseReceiversDisconnectNotified(2000);
            assertEquals(1, aService.getDisconnectedResponseReceivers().size());
            assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aService.getDisconnectedResponseReceivers().get(0).getResponseReceiverId());
         }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClient.getOutputChannel().closeConnection();
            aService.getInputChannel().stopListening();

            // EneterTrace.StopProfiler();
            Thread.sleep(200);
        }
    }
    
    @Test
    public void A14_ResponseReceiverReconnects_AfterDisconnect() throws Exception
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
    public void A15_ResponseReceiverReconnects_AfterStopListening() throws Exception
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
    public void A16_RequestResponse_100_ConstantlyInterrupted() throws Exception
    {
        final IDuplexOutputChannel aDuplexOutputChannel = MessagingSystem.createDuplexOutputChannel(ChannelId);
        final IDuplexInputChannel aDuplexInputChannel = MessagingSystem.createDuplexInputChannel(ChannelId);

        Field aMember = aDuplexInputChannel.getClass().getDeclaredField("myInputChannel");
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
    
    private void sendMessageReceiveResponse(String channelId, Object message, Object responseMessage,
            int numberOfClients, int numberOfMessages,
            int openConnectionTimeout,
            int allMessagesReceivedTimeout) throws Exception
    {
        ClientMockFarm aClientFarm = new ClientMockFarm(MessagingSystem, channelId, numberOfClients);

        ServiceMock aService = new ServiceMock(MessagingSystem, channelId);
        aService.doOnMessageReceived_SendResponse(responseMessage);

        try
        {
            //EneterTrace.StartProfiler();

            aService.getInputChannel().startListening();
            aClientFarm.openConnectionsAsync();

            aClientFarm.waitUntilAllConnectionsAreOpen(openConnectionTimeout);
            aService.waitUntilResponseReceiversConnectNotified(numberOfClients, openConnectionTimeout);
            assertEquals(aClientFarm.getClients().size(), aService.getConnectedResponseReceivers().size());

            for (final ClientMock aClient : aClientFarm.getClients())
            {
                assertTrue(EnumerableExt.any(aService.getConnectedResponseReceivers(), new IFunction1<Boolean, ResponseReceiverEventArgs>()
                {
                    @Override
                    public Boolean invoke(ResponseReceiverEventArgs x) throws Exception
                    {
                        return x.getResponseReceiverId().equals(aClient.getOutputChannel().getResponseReceiverId());
                    }
                }));
            }

            PerformanceTimer aStopWatch = new PerformanceTimer();
            aStopWatch.start();

            aClientFarm.sendMessageAsync(message, numberOfMessages);
            aClientFarm.waitUntilAllResponsesAreReceived(numberOfMessages, allMessagesReceivedTimeout);

            aStopWatch.stop();

            // Wait little bit more for case there is an error that more messages are sent.
            Thread.sleep(500);

            assertEquals(numberOfMessages * numberOfClients, aService.getReceivedMessages().size());
            assertEquals(numberOfMessages * numberOfClients, aClientFarm.getReceivedResponses().size());
            for (DuplexChannelMessageEventArgs aMessage : aService.getReceivedMessages())
            {
                // Note: IsEqual recognizes byte[] but essertEquals not.
                assertThat(message, IsEqual.equalTo(aMessage.getMessage()));
            }
            for (DuplexChannelMessageEventArgs aResponseMessage : aClientFarm.getReceivedResponses())
            {
                assertThat(responseMessage, IsEqual.equalTo(aResponseMessage.getMessage()));
            }
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClientFarm.closeAllConnections();
            aService.getInputChannel().stopListening();

            // EneterTrace.StopProfiler();
            Thread.sleep(200);
        }
    }
    
    @Test
    public void A17_Online_Offline_Events() throws Exception
    {
        // Duplex output channel without queue - it will not try to reconnect.
        IBufferedDuplexOutputChannel aDuplexOutputChannel = (IBufferedDuplexOutputChannel)MessagingSystem.createDuplexOutputChannel(ChannelId);
        IBufferedDuplexInputChannel aDuplexInputChannel = (IBufferedDuplexInputChannel)MessagingSystem.createDuplexInputChannel(ChannelId);

        AutoResetEvent aConnectionsCompletedEvent = new AutoResetEvent(false);
        aDuplexInputChannel.responseReceiverConnected().subscribe((x, y) -> aConnectionsCompletedEvent.set());
        
        AutoResetEvent aResponseReceiverOnline = new AutoResetEvent(false);
        aDuplexInputChannel.responseReceiverOnline().subscribe((x, y) -> aResponseReceiverOnline.set());

        AutoResetEvent aResponseReceiverOffline = new AutoResetEvent(false);
        aDuplexInputChannel.responseReceiverOffline().subscribe((x, y) -> aResponseReceiverOffline.set());

        AutoResetEvent aOnlineIsRaised = new AutoResetEvent(false);
        final boolean[] aOnlineStateAfterOnline = { false };
        aDuplexOutputChannel.connectionOnline().subscribe((x, y) ->
        {
            aOnlineStateAfterOnline[0] = aDuplexOutputChannel.isOnline();
            aOnlineIsRaised.set();   
        });

        AutoResetEvent aOfflineIsRaised = new AutoResetEvent(false);
        final boolean[] aOnlineStateAfterOffline = { false };
        aDuplexOutputChannel.connectionOffline().subscribe((x, y) ->
        {
            aOnlineStateAfterOffline[0] = aDuplexOutputChannel.isOnline();
            aOfflineIsRaised.set();
        });

        try
        {
            aDuplexOutputChannel.openConnection();

            if (!aOfflineIsRaised.waitOne(1000))
            {
                fail("Offline event was not raised.");
            }
            assertFalse(aOnlineStateAfterOffline[0]);

            // start listening
            aDuplexInputChannel.startListening();

            if (!aOnlineIsRaised.waitOne(1000))
            {
                fail("Online event was not raised.");
            }
            assertTrue(aOnlineStateAfterOnline[0]);

            if (!aResponseReceiverOnline.waitOne(1000))
            {
                fail("ResponseReceiverOnline event was not raised.");
            }

            // Wait until the connection is open.
            if (!aConnectionsCompletedEvent.waitOne(1000))
            {
                fail("Connection was not open.");
            }

            // Disconnect the response receiver.
            aDuplexInputChannel.disconnectResponseReceiver(aDuplexOutputChannel.getResponseReceiverId());

            if (!aOfflineIsRaised.waitOne(1000))
            {
                fail("Offline event was not raised after disconnection.");
            }
            assertFalse(aOnlineStateAfterOffline[0]);

            if (aResponseReceiverOffline.waitOne(500))
            {
                fail("ResponseReceiverOffline event shall NOT be raised if DisconnectResponseReceiver was called.");
            }


            // The duplex output channel will try to connect again, therefore wait until connected.
            aConnectionsCompletedEvent.waitOne();


            if (!aOnlineIsRaised.waitOne(1000))
            {
                fail("Online event was not raised after reconnection.");
            }
            assertTrue(aOnlineStateAfterOnline[0]);

            if (!aResponseReceiverOnline.waitOne(1000))
            {
                fail("ResponseReceiverOnline event was not raised.");
            }

            // duplex output channel closes the connection.
            aDuplexOutputChannel.closeConnection();


            if (!aResponseReceiverOffline.waitOne(1000))
            {
                fail("ResponseReceiverOffline event was not raised.");
            }

            if (aOfflineIsRaised.waitOne(500))
            {
                fail("Offline event shall NOT be raised after CloseConnection().");
            }
            assertFalse(aOnlineStateAfterOffline[0]);

        }
        finally
        {
            aDuplexOutputChannel.closeConnection();
            aDuplexInputChannel.stopListening();
        }
    }
    
    private void sendOfflineMessageReceiveResponse(String channelId, Object message, Object responseMessage,
            int numberOfClients, int numberOfMessages,
            int openConnectionTimeout,
            int allMessagesReceivedTimeout) throws Exception
    {
        ClientMockFarm aClientFarm = new ClientMockFarm(MessagingSystem, channelId, numberOfClients);

        ServiceMock aService = new ServiceMock(MessagingSystem, channelId);
        aService.doOnMessageReceived_SendResponse(responseMessage);

        try
        {
            //EneterTrace.StartProfiler();

            aClientFarm.openConnectionsAsync();

            Thread.sleep(500);

            aService.getInputChannel().startListening();
            aClientFarm.waitUntilAllConnectionsAreOpen(openConnectionTimeout);
            aService.waitUntilResponseReceiversConnectNotified(numberOfClients, openConnectionTimeout);
            assertEquals(aClientFarm.getClients().size(), aService.getConnectedResponseReceivers().size());

            for (final ClientMock aClient : aClientFarm.getClients())
            {
                assertTrue(EnumerableExt.any(aService.getConnectedResponseReceivers(), new IFunction1<Boolean, ResponseReceiverEventArgs>()
                {
                    @Override
                    public Boolean invoke(ResponseReceiverEventArgs x)
                            throws Exception
                    {
                        return x.getResponseReceiverId().equals(aClient.getOutputChannel().getResponseReceiverId());
                    }
                }));
            }

            PerformanceTimer aStopWatch = new PerformanceTimer();
            aStopWatch.start();

            aClientFarm.sendMessageAsync(message, numberOfMessages);
            aClientFarm.waitUntilAllResponsesAreReceived(numberOfMessages, allMessagesReceivedTimeout);

            aStopWatch.stop();

            // Wait little bit more for case there is an error that more messages are sent.
            Thread.sleep(500);

            assertEquals(numberOfMessages * numberOfClients, aService.getReceivedMessages().size());
            assertEquals(numberOfMessages * numberOfClients, aClientFarm.getReceivedResponses().size());
            for (DuplexChannelMessageEventArgs aMessage : aService.getReceivedMessages())
            {
                assertEquals(message, aMessage.getMessage());
            }
            for (DuplexChannelMessageEventArgs aResponseMessage : aClientFarm.getReceivedResponses())
            {
                assertEquals(responseMessage, aResponseMessage.getMessage());
            }
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClientFarm.closeAllConnections();
            aService.getInputChannel().stopListening();

            //EneterTrace.StopProfiler();
            Thread.sleep(500);
        }
    }
    
    private void sendOfflineResponse(String channelId, Object responseMessage,
            int numberOfClients, int numberOfMessages,
            int openConnectionTimeout,
            int allMessagesReceivedTimeout) throws Exception
    {
        ClientMockFarm aClientFarm = new ClientMockFarm(MessagingSystem, channelId, numberOfClients);

        ServiceMock aService = new ServiceMock(MessagingSystem, channelId);
        aService.doOnMessageReceived_SendResponse(responseMessage);

        try
        {
            //EneterTrace.StartProfiler();

            aService.getInputChannel().startListening();

            PerformanceTimer aStopWatch = new PerformanceTimer();
            aStopWatch.start();

            for (ClientMock aClientMock : aClientFarm.getClients())
            {
                for (int i = 0; i < numberOfMessages; ++i)
                {
                    aService.getInputChannel().sendResponseMessage(aClientMock.getOutputChannel().getResponseReceiverId(), responseMessage);
                }
            }

            Thread.sleep(500);

            aClientFarm.openConnectionsAsync();
            aClientFarm.waitUntilAllConnectionsAreOpen(openConnectionTimeout);

            aClientFarm.waitUntilAllResponsesAreReceived(numberOfMessages, allMessagesReceivedTimeout);

            aStopWatch.stop();

            for (final ClientMock aClient : aClientFarm.getClients())
            {
                assertTrue(EnumerableExt.any(aService.getConnectedResponseReceivers(), new IFunction1<Boolean, ResponseReceiverEventArgs>()
                {
                    @Override
                    public Boolean invoke(ResponseReceiverEventArgs x)
                            throws Exception
                    {
                        return x.getResponseReceiverId().equals(aClient.getOutputChannel().getResponseReceiverId());
                    }
                }));
            }

            // Wait little bit more for case there is an error that more messages are sent.
            Thread.sleep(500);

            assertEquals(numberOfMessages * numberOfClients, aClientFarm.getReceivedResponses().size());
            for (DuplexChannelMessageEventArgs aResponseMessage : aClientFarm.getReceivedResponses())
            {
                assertEquals(responseMessage, aResponseMessage.getMessage());
            }
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClientFarm.closeAllConnections();
            aService.getInputChannel().stopListening();

            //EneterTrace.StopProfiler();
            Thread.sleep(500);
        }
    }
    
    private void sendBroadcastResponseMessage(String channelId, Object broadcastMessage,
            int numberOfClients, int numberOfMessages,
            int openConnectionTimeout,
            int allMessagesReceivedTimeout) throws Exception
    {
        ClientMockFarm aClientFarm = new ClientMockFarm(MessagingSystem, channelId, numberOfClients);
        ServiceMock aService = new ServiceMock(MessagingSystem, channelId);

        try
        {
            aService.getInputChannel().startListening();
            aClientFarm.openConnectionsAsync();

            aClientFarm.waitUntilAllConnectionsAreOpen(openConnectionTimeout);
            aService.waitUntilResponseReceiversConnectNotified(numberOfClients, openConnectionTimeout);
            assertEquals(aClientFarm.getClients().size(), aService.getConnectedResponseReceivers().size());
            for (final ClientMock aClient : aClientFarm.getClients())
            {
                assertTrue(EnumerableExt.any(aService.getConnectedResponseReceivers(), new IFunction1<Boolean, ResponseReceiverEventArgs>()
                {
                    @Override
                    public Boolean invoke(ResponseReceiverEventArgs x)
                            throws Exception
                    {
                        return x.getResponseReceiverId().equals(aClient.getOutputChannel().getResponseReceiverId());
                    }
                }));
            }

            PerformanceTimer aStopWatch = new PerformanceTimer();
            aStopWatch.start();

            for (int i = 0; i < numberOfMessages; ++i)
            {
                aService.getInputChannel().sendResponseMessage("*", broadcastMessage);
            }
            aClientFarm.waitUntilAllResponsesAreReceived(numberOfMessages, allMessagesReceivedTimeout);

            aStopWatch.stop();

            for (DuplexChannelMessageEventArgs aResponseMessage : aClientFarm.getReceivedResponses())
            {
                assertEquals(broadcastMessage, aResponseMessage.getMessage());
            }
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClientFarm.closeAllConnections();
            aService.getInputChannel().stopListening();

            //EneterTrace.StopProfiler();
            Thread.sleep(500);
        }
    }
    
    
    private void sendOfflineBroadcastResponseMessage(String channelId, Object broadcastMessage,
            int numberOfClients, int numberOfMessages,
            int openConnectionTimeout,
            int allMessagesReceivedTimeout) throws Exception
    {
        ClientMockFarm aClientFarm = new ClientMockFarm(MessagingSystem, channelId, numberOfClients);
        ServiceMock aService = new ServiceMock(MessagingSystem, channelId);

        try
        {
            aService.getInputChannel().startListening();

            // Send broadcasts.
            for (int i = 0; i < numberOfMessages; ++i)
            {
                aService.getInputChannel().sendResponseMessage("*", broadcastMessage);
            }

            Thread.sleep(500);

            aClientFarm.openConnectionsAsync();

            aClientFarm.waitUntilAllConnectionsAreOpen(openConnectionTimeout);
            aService.waitUntilResponseReceiversConnectNotified(numberOfClients, openConnectionTimeout);
            assertEquals(aClientFarm.getClients().size(), aService.getConnectedResponseReceivers().size());
            for (final ClientMock aClient : aClientFarm.getClients())
            {
                assertTrue(EnumerableExt.any(aService.getConnectedResponseReceivers(), new IFunction1<Boolean, ResponseReceiverEventArgs>()
                {
                    @Override
                    public Boolean invoke(ResponseReceiverEventArgs x)
                            throws Exception
                    {
                        return x.getResponseReceiverId().equals(aClient.getOutputChannel().getResponseReceiverId());
                    }
                })); 
            }

            PerformanceTimer aStopWatch = new PerformanceTimer();
            aStopWatch.start();

            aClientFarm.waitUntilAllResponsesAreReceived(numberOfMessages, allMessagesReceivedTimeout);

            aStopWatch.stop();

            for (DuplexChannelMessageEventArgs aResponseMessage : aClientFarm.getReceivedResponses())
            {
                assertEquals(broadcastMessage, aResponseMessage.getMessage());
            }
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClientFarm.closeAllConnections();
            aService.getInputChannel().stopListening();

            //EneterTrace.StopProfiler();
            Thread.sleep(500);
        }
    }
    
    
    
    protected String ChannelId;
    protected IMessagingSystemFactory MessagingSystem;
    protected int ConnectionInterruptionFrequency;
    
    protected Object myRequestMessage = "Message";
    protected Object myResponseMessage = "Response";

    protected Object myMessage_10MB = RandomDataGenerator.getString(10000000);
}

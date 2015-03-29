package eneter.messaging.messagingsystems;

import static org.junit.Assert.*;

import helper.*;

import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.net.system.*;
import eneter.net.system.internal.IMethod2;
import eneter.net.system.linq.internal.EnumerableExt;


public abstract class MessagingSystemBaseTester
{
    public MessagingSystemBaseTester()
    {
        ChannelId = "Channel1";
    }
    
    
    @Test
    public void Duplex_01_Send1()
            throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 1, 1, 1000, 500);
    }
    
    @Test
    public void Duplex_02_Send500()
            throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 1, 500, 1000, 1000);
    }
    
    //@Ignore
    @Test
    public void Duplex_03_Send1_10MB() throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myMessage_10MB, myMessage_10MB, 1, 1, 1000, 5000);
    }
    
    @Test
    public void Duplex_04_Send50000() throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 1, 50000, 500, 20000);
    }
    
    @Test
    public void Duplex_05_Send50_10Prallel()
            throws Exception
    {
        sendMessageReceiveResponse(ChannelId, myRequestMessage, myResponseMessage, 10, 50, 1000, 10000);
    }
    
    @Test
    public void Duplex_05a_SendBroadcastResponse_50_10Clients()
            throws Exception
    {
        sendBroadcastResponseMessage(ChannelId, myResponseMessage, 10, 50, 1000, 2000);
    }
    
    @Test
    public void Duplex_06_OpenCloseConnection()
        throws Throwable
    {
        ClientMock aClient = new ClientMock(MessagingSystemFactory, ChannelId);
        ServiceMock aService = new ServiceMock(MessagingSystemFactory, ChannelId);

        try
        {
            aService.getInputChannel().startListening();

            // Open the connection.
            aClient.getOutputChannel().openConnection();
            assertTrue(aClient.getOutputChannel().isConnected());

            // handling open connection on the client side.
            EneterTrace.info("1");
            aClient.waitUntilConnectionOpenIsNotified(1000);
            assertEquals(aClient.getOutputChannel().getChannelId(), aClient.getNotifiedOpenConnection().getChannelId());
            assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aClient.getNotifiedOpenConnection().getResponseReceiverId());

            // handling open connection on the service side.
            EneterTrace.info("2");
            aService.waitUntilResponseReceiversConnectNotified(1, 1000);
            assertEquals(1, aService.getConnectedResponseReceivers().size());
            if (CompareResponseReceiverId)
            {
                assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aService.getConnectedResponseReceivers().get(0).getResponseReceiverId());
            }

            aClient.getOutputChannel().closeConnection();
            assertFalse(aClient.getOutputChannel().isConnected());

            EneterTrace.info("3");
            aClient.waitUntilConnectionClosedIsNotified(1000);
            assertEquals(aClient.getOutputChannel().getChannelId(), aClient.getNotifiedCloseConnection().getChannelId());
            assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aClient.getNotifiedCloseConnection().getResponseReceiverId());

            EneterTrace.info("4");
            aService.waitUntilAllResponseReceiversDisconnectNotified(1000);
            assertEquals(1, aService.getDisconnectedResponseReceivers().size());
            if (CompareResponseReceiverId)
            {
                assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aService.getDisconnectedResponseReceivers().get(0).getResponseReceiverId());
            }
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClient.getOutputChannel().closeConnection();
            aService.getInputChannel().stopListening();

            // Wait for traces.
            Thread.sleep(100);
        }
    }
    

    @Test
    public void Duplex_06_OpenCloseOpenSend() throws Exception
    {
        ClientMock aClient = new ClientMock(MessagingSystemFactory, ChannelId);
        ServiceMock aService = new ServiceMock(MessagingSystemFactory, ChannelId);
        aService.doOnMessageReceived_SendResponse(myResponseMessage);

        try
        {
            aService.getInputChannel().startListening();

            // Client opens the connection.
            aClient.getOutputChannel().openConnection();
            assertTrue(aClient.getOutputChannel().isConnected());

            aClient.waitUntilConnectionOpenIsNotified(1000);
            assertEquals(aClient.getOutputChannel().getChannelId(), aClient.getNotifiedOpenConnection().getChannelId());
            assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aClient.getNotifiedOpenConnection().getResponseReceiverId());

            aService.waitUntilResponseReceiversConnectNotified(1, 1000);
            assertEquals(1, aService.getConnectedResponseReceivers().size());
            if (CompareResponseReceiverId)
            {
                assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aService.getConnectedResponseReceivers().get(0).getResponseReceiverId());
            }

            // Client closes the connection.
            aClient.getOutputChannel().closeConnection();
            assertFalse(aClient.getOutputChannel().isConnected());

            aClient.waitUntilConnectionClosedIsNotified(1000);
            assertEquals(aClient.getOutputChannel().getChannelId(), aClient.getNotifiedCloseConnection().getChannelId());
            assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aClient.getNotifiedCloseConnection().getResponseReceiverId());

            aService.waitUntilAllResponseReceiversDisconnectNotified(1000);
            assertEquals(1, aService.getDisconnectedResponseReceivers().size());
            if (CompareResponseReceiverId)
            {
                assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aService.getDisconnectedResponseReceivers().get(0).getResponseReceiverId());
            }

            aClient.clearTestResults();
            aService.clearTestResults();


            // Client opens the connection 2nd time.
            aClient.getOutputChannel().openConnection();
            assertTrue(aClient.getOutputChannel().isConnected());

            aClient.waitUntilConnectionOpenIsNotified(1000);
            assertEquals(aClient.getOutputChannel().getChannelId(), aClient.getNotifiedOpenConnection().getChannelId());
            assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aClient.getNotifiedOpenConnection().getResponseReceiverId());

            aService.waitUntilResponseReceiversConnectNotified(1, 1000);
            assertEquals(1, aService.getConnectedResponseReceivers().size());
            if (CompareResponseReceiverId)
            {
                assertEquals(aClient.getOutputChannel().getResponseReceiverId(), aService.getConnectedResponseReceivers().get(0).getResponseReceiverId());
            }

            // Client sends the message.
            aClient.getOutputChannel().sendMessage(myRequestMessage);

            aClient.waitUntilResponseMessagesAreReceived(1, 1000);

            assertEquals(myRequestMessage, aService.getReceivedMessages().get(0).getMessage());
            assertEquals(myResponseMessage, aClient.getReceivedMessages().get(0).getMessage());
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClient.getOutputChannel().closeConnection();
            aService.getInputChannel().stopListening();

            // Wait for traces.
            Thread.sleep(100);
        }
    }

    @Test
    public void Duplex_07_OpenConnection_if_InputChannelNotStarted() throws Exception
    {
        IDuplexOutputChannel anOutputChannel = MessagingSystemFactory.createDuplexOutputChannel(ChannelId);

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
        final ClientMock aClient = new ClientMock(MessagingSystemFactory, ChannelId);

        final boolean[] anIsConnected = { false };
        aClient.doOnConnectionClosed(new IMethod2<Object, DuplexChannelEventArgs>()
        {
            @Override
            public void invoke(Object t1, DuplexChannelEventArgs t2) throws Exception
            {
                anIsConnected[0] = aClient.getOutputChannel().isConnected();
                aClient.getOutputChannel().openConnection();
            }
        });
                
        ServiceMock aService = new ServiceMock(MessagingSystemFactory, ChannelId);

        try
        {
            aService.getInputChannel().startListening();

            // Client opens the connection.
            aClient.getOutputChannel().openConnection();

            aClient.waitUntilConnectionOpenIsNotified(1000);
            assertFalse(anIsConnected[0]);

            aService.waitUntilResponseReceiversConnectNotified(1, 1000);
            String aConnectedResponseReceiverId = aService.getConnectedResponseReceivers().get(0).getResponseReceiverId();

            aClient.clearTestResults();
            aService.clearTestResults();

            // Service disconnects the client.
            aService.getInputChannel().disconnectResponseReceiver(aConnectedResponseReceiverId);

            aService.waitUntilResponseRecieverIdDisconnectNotified(aConnectedResponseReceiverId, 1000);
            aClient.waitUntilConnectionClosedIsNotified(1000);
            assertEquals(aConnectedResponseReceiverId, aService.getDisconnectedResponseReceivers().get(0).getResponseReceiverId());

            // Client should open the connection again.
            aClient.waitUntilConnectionOpenIsNotified(1000);

            if (MessagingSystemFactory instanceof SynchronousMessagingSystemFactory == false)
            {
                aService.waitUntilResponseReceiversConnectNotified(1, 1000);
            }
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClient.getOutputChannel().closeConnection();
            aService.getInputChannel().stopListening();

            // Wait for traces.
            Thread.sleep(500);
        }
    }
    
    @Test
    public void Duplex_09_StopListening_SendMessage() throws Exception
    {
        ClientMock aClient = new ClientMock(MessagingSystemFactory, ChannelId);
        ServiceMock aService = new ServiceMock(MessagingSystemFactory, ChannelId);

        try
        {
            aService.getInputChannel().startListening();

            aClient.getOutputChannel().openConnection();

            aService.waitUntilResponseReceiversConnectNotified(1, 1000);

            aService.getInputChannel().stopListening();
            assertFalse(aService.getInputChannel().isListening());

            boolean isSomeException = false;
            try
            {
                // Try to send a message via the duplex output channel.
                aClient.getOutputChannel().sendMessage(myRequestMessage);
            }
            catch (Exception err)
            {
                // Because the duplex input channel is not listening the sending must
                // fail with an exception. The type of the exception depends from the type of messaging system.
                isSomeException = true;
            }

            assertTrue(isSomeException);
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClient.getOutputChannel().closeConnection();
            aService.getInputChannel().stopListening();

            // Wait for traces.
            Thread.sleep(100);
        }
    }
    
    @Test
    public void Duplex_09_StopListeing() throws Exception
    {
        ClientMockFarm aClients = new ClientMockFarm(MessagingSystemFactory, ChannelId, 3);
        ServiceMock aService = new ServiceMock(MessagingSystemFactory, ChannelId);

        try
        {
            aService.getInputChannel().startListening();

            aClients.openConnectionsAsync();
            assertTrue(aClients.getClients().get(0).getOutputChannel().isConnected());
            assertTrue(aClients.getClients().get(1).getOutputChannel().isConnected());
            assertTrue(aClients.getClients().get(2).getOutputChannel().isConnected());

            aClients.waitUntilAllConnectionsAreOpen(1000);
            aService.waitUntilResponseReceiversConnectNotified(3, 1000);

            aService.getInputChannel().stopListening();
            assertFalse(aService.getInputChannel().isListening());

            aService.waitUntilAllResponseReceiversDisconnectNotified(1000);
            aClients.waitUntilAllConnectionsAreClosed(1000);
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClients.closeAllConnections();
            aService.getInputChannel().stopListening();

            // Wait for traces.
            Thread.sleep(100);
        }
    }
    
    @Test
    public void Duplex_10_DisconnectResponseReceiver()
        throws Exception
    {
        ClientMock aClient = new ClientMock(MessagingSystemFactory, ChannelId);
        ServiceMock aService = new ServiceMock(MessagingSystemFactory, ChannelId);

        try
        {
            aService.getInputChannel().startListening();

            aClient.getOutputChannel().openConnection();

            aService.waitUntilResponseReceiversConnectNotified(1, 1000);
            aClient.waitUntilConnectionOpenIsNotified(1000);
            String aConnectedResponseReceiverId = aService.getConnectedResponseReceivers().get(0).getResponseReceiverId();

            aService.getInputChannel().disconnectResponseReceiver(aService.getConnectedResponseReceivers().get(0).getResponseReceiverId());

            aClient.waitUntilConnectionClosedIsNotified(1000);
            aService.waitUntilAllResponseReceiversDisconnectNotified(1000);

            assertEquals(aConnectedResponseReceiverId, aService.getDisconnectedResponseReceivers().get(0).getResponseReceiverId());
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClient.getOutputChannel().closeConnection();
            aService.getInputChannel().stopListening();

            // Wait for traces.
            Thread.sleep(100);
        }
    }
    
    @Test
    public void Duplex_11_CloseConnection()
        throws Exception
    {
        ClientMockFarm aClients = new ClientMockFarm(MessagingSystemFactory, ChannelId, 2);
        ServiceMock aService = new ServiceMock(MessagingSystemFactory, ChannelId);

        try
        {
            aService.getInputChannel().startListening();

            aClients.openConnectionsAsync();
            assertTrue(aClients.getClients().get(0).getOutputChannel().isConnected());
            assertTrue(aClients.getClients().get(1).getOutputChannel().isConnected());

            aClients.waitUntilAllConnectionsAreOpen(1000);
            aService.waitUntilResponseReceiversConnectNotified(2, 1000);
            String aResponseReceiverId1 = aService.getConnectedResponseReceivers().get(0).getResponseReceiverId();

            // Cient 1 closes the connection.
            aClients.getClients().get(0).getOutputChannel().closeConnection();
            assertFalse(aClients.getClients().get(0).getOutputChannel().isConnected());

            aClients.getClients().get(0).waitUntilConnectionClosedIsNotified(1000);
            aService.waitUntilResponseRecieverIdDisconnectNotified(aResponseReceiverId1, 1000);
            if (CompareResponseReceiverId)
            {
                assertEquals(aClients.getClients().get(0).getOutputChannel().getResponseReceiverId(), aService.getDisconnectedResponseReceivers().get(0).getResponseReceiverId());
            }

            // Client 2 can send message.
            aClients.getClients().get(1).getOutputChannel().sendMessage(myRequestMessage);

            aService.waitUntilMessagesAreReceived(1, 1000);

            assertEquals(myRequestMessage, aService.getReceivedMessages().get(0).getMessage());
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClients.closeAllConnections();
            aService.getInputChannel().stopListening();

            // Wait for traces.
            Thread.sleep(100);
        }
    }
    
    @Test
    public void Duplex_12_CloseFromConnectionOpened()
        throws Exception
    {
        final ClientMock aClient = new ClientMock(MessagingSystemFactory, ChannelId);

        aClient.doOnConnectionOpen(new IMethod2<Object, DuplexChannelEventArgs>()
        {
            @Override
            public void invoke(Object t1, DuplexChannelEventArgs t2)
                    throws Exception
            {
                aClient.getOutputChannel().closeConnection();
            }
        });

        ServiceMock aService = new ServiceMock(MessagingSystemFactory, ChannelId);

        try
        {
            aService.getInputChannel().startListening();

            aClient.getOutputChannel().openConnection();

            aClient.waitUntilConnectionOpenIsNotified(1000);

            if (MessagingSystemFactory instanceof SynchronousMessagingSystemFactory == false)
            {
                aService.waitUntilResponseReceiversConnectNotified(1, 5000);
            }

            // Client is disconnected.
            aClient.waitUntilConnectionClosedIsNotified(1000);

            // Client should be disconnected from the event handler.
            aService.waitUntilAllResponseReceiversDisconnectNotified(2000);
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClient.getOutputChannel().closeConnection();
            aService.getInputChannel().stopListening();

            // Wait for traces.
            Thread.sleep(100);
        }
    }
    
    @Test
    public void Duplex_13_DisconnectFromResponseReceiverConnected()
        throws Exception
    {
        ClientMock aClient = new ClientMock(MessagingSystemFactory, ChannelId);
        final ServiceMock aService = new ServiceMock(MessagingSystemFactory, ChannelId);

        aService.doOnResponseReceiverConnected(new IMethod2<Object, ResponseReceiverEventArgs>()
        {
            @Override
            public void invoke(Object x, ResponseReceiverEventArgs y)
                    throws Exception
            {
                aService.getInputChannel().disconnectResponseReceiver(y.getResponseReceiverId());
            }
        });

        try
        {
            aService.getInputChannel().startListening();

            // The ecent will try to close connection.
            aClient.getOutputChannel().openConnection();

            aClient.waitUntilConnectionOpenIsNotified(1000);

            aService.waitUntilAllResponseReceiversDisconnectNotified(1000);

            aClient.waitUntilConnectionClosedIsNotified(1000);
        }
        finally
        {
            EneterTrace.debug("CLEANING AFTER TEST");

            aClient.getOutputChannel().closeConnection();
            aService.getInputChannel().stopListening();

            // Wait for traces.
            Thread.sleep(100);
        }
    }
    
    protected void sendMessageReceiveResponse(String channelId, Object message, Object responseMessage,
            int numberOfClients, int numberOfMessages,
            int openConnectionTimeout,
            int allMessagesReceivedTimeout) throws Exception
    {
        ClientMockFarm aClientFarm = new ClientMockFarm(MessagingSystemFactory, channelId, numberOfClients);

        ServiceMock aService = new ServiceMock(MessagingSystemFactory, channelId);
        aService.doOnMessageReceived_SendResponse(responseMessage);

        try
        {
            //EneterTrace.StartProfiler();

            aService.getInputChannel().startListening();
            aClientFarm.openConnectionsAsync();

            aClientFarm.waitUntilAllConnectionsAreOpen(openConnectionTimeout);
            aService.waitUntilResponseReceiversConnectNotified(numberOfClients, openConnectionTimeout);
            assertEquals(aClientFarm.getClients().size(), aService.getConnectedResponseReceivers().size());

            if (CompareResponseReceiverId)
            {
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

            // EneterTrace.StopProfiler();
            Thread.sleep(200);
        }
    }
    
    private void sendBroadcastResponseMessage(String channelId, Object broadcastMessage,
            int numberOfClients, int numberOfMessages,
            int openConnectionTimeout,
            int allMessagesReceivedTimeout) throws Exception
{
        ClientMockFarm aClientFarm = new ClientMockFarm(MessagingSystemFactory, channelId, numberOfClients);
        ServiceMock aService = new ServiceMock(MessagingSystemFactory, channelId);

        try
        {
            aService.getInputChannel().startListening();
            aClientFarm.openConnectionsAsync();

            aClientFarm.waitUntilAllConnectionsAreOpen(openConnectionTimeout);
            aService.waitUntilResponseReceiversConnectNotified(numberOfClients, openConnectionTimeout);
            assertEquals(aClientFarm.getClients().size(), aService.getConnectedResponseReceivers().size());
            if (CompareResponseReceiverId)
            {
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

            // EneterTrace.StopProfiler();
            Thread.sleep(500);
        }
    }

    

    protected IMessagingSystemFactory MessagingSystemFactory;
    protected String ChannelId;
    
    protected boolean CompareResponseReceiverId = true;

    protected Object myRequestMessage = "Message";
    protected Object myResponseMessage = "Response";
    
    private static String myMessage_10MB = RandomDataGenerator.getString(10000000);
}

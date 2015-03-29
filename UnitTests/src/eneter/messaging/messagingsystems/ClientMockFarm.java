package eneter.messaging.messagingsystems;

import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.internal.IMethod2;
import eneter.net.system.threading.internal.ThreadPool;

public class ClientMockFarm
{
    public ClientMockFarm(IMessagingSystemFactory messaging, String channelId, int numberOfClients) throws Exception
    {
        for (int i = 0; i < numberOfClients; ++i)
        {
            ClientMock aClient = new ClientMock(messaging, channelId);
            myClients.add(aClient);
        }
    }
    
    public void ClearTestResults()
    {
        for (ClientMock aClient : myClients)
        {
            aClient.clearTestResults();
        }
    }
    
    public void openConnectionsAsync() throws Exception
    {
        for (ClientMock aClient : myClients)
        {
            final ClientMock aC = aClient;
            Runnable aW = new Runnable()
            {
                @Override
                public void run()
                {
                    //EneterTrace.Info("CONNECT CLIENT");
                    try
                    {
                        aC.getOutputChannel().openConnection();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error("Failed to open connection.", err);
                    }
                }
            };
            ThreadPool.queueUserWorkItem(aW);

            Thread.sleep(2);
        }
    }
    
    public void doOnConnectionOpen(IMethod2<Object, DuplexChannelEventArgs> doOnConnectionOpen)
    {
        for (ClientMock aClient : myClients)
        {
            aClient.doOnConnectionOpen(doOnConnectionOpen);
        }
    }
    public void doOnConnectionOpen_CloseConnection()
    {
        for (ClientMock aClient : myClients)
        {
            aClient.doOnConnectionOpen_CloseConnection();
        }
    }
    
    public void waitUntilAllConnectionsAreOpen(int milliseconds) throws Exception
    {
        for (ClientMock aClient : myClients)
        {
            aClient.waitUntilConnectionOpenIsNotified(milliseconds);
        }
    }
    
    public void closeAllConnections()
    {
        for (ClientMock aClient : myClients)
        {
            aClient.getOutputChannel().closeConnection();
        }
    }
    
    public void waitUntilAllConnectionsAreClosed(int milliseconds) throws Exception
    {
        for (ClientMock aClient : myClients)
        {
            aClient.waitUntilConnectionClosedIsNotified(milliseconds);
        }
    }
    
    public void doOnConnectionClosed(IMethod2<Object, DuplexChannelEventArgs> doOnConnectionClosed)
    {
        for (ClientMock aClient : myClients)
        {
            aClient.doOnConnectionClosed(doOnConnectionClosed);
        }
    }
    public void doOnConnectionClosed_OpenConnection()
    {
        for (ClientMock aClient : myClients)
        {
            aClient.doOnConnectionClosed_OpenConnection();
        }
    }
    
    public void sendMessageAsync(final Object message, int repeat) throws Exception
    {
        final int[] aRepeat = { repeat };
        for (ClientMock aClient : myClients)
        {
            final ClientMock aC = aClient;
            Runnable aW = new Runnable()
            {
                @Override
                public void run()
                {
                    for (int i = 0; i < aRepeat[0]; ++i)
                    {
                        try
                        {
                            aC.getOutputChannel().sendMessage(message);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error("Failed to send message.", err);
                        }
                    }
                }
            };
            ThreadPool.queueUserWorkItem(aW);

            Thread.sleep(2);
        }
    }
    
    public void waitUntilAllResponsesAreReceived(int numberOfExpectedResponseMessagesPerClient, int milliseconds) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            int anOverallNumberOfExpectedResponseMessages = numberOfExpectedResponseMessagesPerClient * myClients.size();

            try
            {
                for (ClientMock aClient : myClients)
                {
                    aClient.waitUntilResponseMessagesAreReceived(numberOfExpectedResponseMessagesPerClient, milliseconds);
                }
            }
            catch (Exception err)
            {
                EneterTrace.error("Received responses: " + getReceivedResponses().size() + " Expected: " + anOverallNumberOfExpectedResponseMessages);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public ArrayList<ClientMock> getClients() { return myClients; }
    
    public ArrayList<DuplexChannelMessageEventArgs> getReceivedResponses()
    {
        ArrayList<DuplexChannelMessageEventArgs> aResult = new ArrayList<DuplexChannelMessageEventArgs>();
        for (ClientMock aClientMock : myClients)
        {
            aResult.addAll(aClientMock.getReceivedMessages());
        }
        
        return aResult;
    }

    private ArrayList<ClientMock> myClients = new ArrayList<ClientMock>();
}

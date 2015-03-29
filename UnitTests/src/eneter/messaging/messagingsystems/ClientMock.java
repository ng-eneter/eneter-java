package eneter.messaging.messagingsystems;

import helper.EventWaitHandleExt;

import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.internal.IMethod2;
import eneter.net.system.threading.internal.ManualResetEvent;

public class ClientMock
{
    public ClientMock(IMessagingSystemFactory messaging, String channelId) throws Exception
    {
        this(messaging.createDuplexOutputChannel(channelId));
    }
    
    public ClientMock(IDuplexOutputChannel outputChannel)
    {
        myOutputChannel = outputChannel;
        myOutputChannel.connectionOpened().subscribe(myOnConnectionOpened);
        myOutputChannel.connectionClosed().subscribe(myOnConnectionClosed);
        myOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);
    }
    
    public void clearTestResults()
    {
        myConnectionOpenIsNotifiedEvent.reset();
        myConnectionClosedIsNotifiedEvent.reset();
        myResponseMessagesReceivedEvent.reset();

        myReceivedMessages.clear();

        myNotifiedOpenConnection = null;
        myNotifiedCloseConnection = null;
    }
    
    public void waitUntilConnectionOpenIsNotified(int milliseconds) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            EventWaitHandleExt.waitIfNotDebugging(myConnectionOpenIsNotifiedEvent, milliseconds);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void doOnConnectionOpen(IMethod2<Object, DuplexChannelEventArgs> doOnConnectionOpen)
    {
        myDoOnConnectionOpen = doOnConnectionOpen;
    }
    public void doOnConnectionOpen_CloseConnection()
    {
        myDoOnConnectionOpen = new IMethod2<Object, DuplexChannelEventArgs>()
        {
            @Override
            public void invoke(Object t1, DuplexChannelEventArgs t2)
                    throws Exception
            {
                myOutputChannel.closeConnection();
            }
        }; 
    }
    
    public DuplexChannelEventArgs getNotifiedOpenConnection() { return myNotifiedOpenConnection; }
    
    
    
    
    public void waitUntilConnectionClosedIsNotified(int milliseconds) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            EventWaitHandleExt.waitIfNotDebugging(myConnectionClosedIsNotifiedEvent, milliseconds);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void doOnConnectionClosed(IMethod2<Object, DuplexChannelEventArgs> doOnConnectionClosed)
    {
        myDoOnConnectionClosed = doOnConnectionClosed;
    }
    public void doOnConnectionClosed_OpenConnection()
    {
        myDoOnConnectionClosed = new IMethod2<Object, DuplexChannelEventArgs>()
        {
            @Override
            public void invoke(Object x, DuplexChannelEventArgs y)
                    throws Exception
            {
                myOutputChannel.openConnection();
            }
        }; 
    }
    
    public DuplexChannelEventArgs getNotifiedCloseConnection() { return myNotifiedCloseConnection; }
    
    public void waitUntilResponseMessagesAreReceived(int numberOfExpectedResonseMessages, int milliseconds) throws Exception
    {
        synchronized (myReceivedMessages)
        {
            myNumberOfExpectedResponseMessages = numberOfExpectedResonseMessages;
            if (myReceivedMessages.size() == myNumberOfExpectedResponseMessages)
            {
                myResponseMessagesReceivedEvent.set();
            }
            else
            {
                myResponseMessagesReceivedEvent.reset();
            }
        }
        EventWaitHandleExt.waitIfNotDebugging(myResponseMessagesReceivedEvent, milliseconds);
    }
    
    public ArrayList<DuplexChannelMessageEventArgs> getReceivedMessages() { return myReceivedMessages; }
    
    private void onConnectionOpened(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myNotifiedOpenConnection = e;
            myConnectionOpenIsNotifiedEvent.set();
            if (myDoOnConnectionOpen != null)
            {
                try
                {
                    myDoOnConnectionOpen.invoke(sender, e);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Detected exception.", err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onConnectionClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myNotifiedCloseConnection = e;
            myConnectionClosedIsNotifiedEvent.set();
            if (myDoOnConnectionClosed != null)
            {
                try
                {
                    myDoOnConnectionClosed.invoke(sender, e);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Detected exception.", err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myReceivedMessages)
            {
                myReceivedMessages.add(e);
                if (myReceivedMessages.size() == myNumberOfExpectedResponseMessages)
                {
                    myResponseMessagesReceivedEvent.set();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public IDuplexOutputChannel getOutputChannel() { return myOutputChannel; }

    private ManualResetEvent myConnectionOpenIsNotifiedEvent = new ManualResetEvent(false);
    private IMethod2<Object, DuplexChannelEventArgs> myDoOnConnectionOpen;

    private ManualResetEvent myConnectionClosedIsNotifiedEvent = new ManualResetEvent(false);
    private IMethod2<Object, DuplexChannelEventArgs> myDoOnConnectionClosed;

    private ManualResetEvent myResponseMessagesReceivedEvent = new ManualResetEvent(false);
    private ArrayList<DuplexChannelMessageEventArgs> myReceivedMessages = new ArrayList<DuplexChannelMessageEventArgs>();
    private int myNumberOfExpectedResponseMessages;
    
    private DuplexChannelEventArgs myNotifiedOpenConnection;
    private DuplexChannelEventArgs myNotifiedCloseConnection;
    private IDuplexOutputChannel myOutputChannel;
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionOpened = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionOpened(sender, e);
        }
    };
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionClosed = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionClosed(sender, e);
        }
    };
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnResponseMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onResponseMessageReceived(sender, e);
        }
    };
}

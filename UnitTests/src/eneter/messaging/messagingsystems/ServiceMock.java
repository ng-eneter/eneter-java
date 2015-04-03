package eneter.messaging.messagingsystems;

import helper.EventWaitHandleExt;

import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.IFunction1;
import eneter.net.system.collections.generic.internal.ArrayListExt;
import eneter.net.system.internal.IMethod2;
import eneter.net.system.linq.internal.EnumerableExt;
import eneter.net.system.threading.internal.ManualResetEvent;

public class ServiceMock
{
    public ServiceMock(IMessagingSystemFactory messaging, String channelId) throws Exception
    {
        myInputChannel = messaging.createDuplexInputChannel(channelId);
        myInputChannel.responseReceiverConnected().subscribe(myOnResponseReceiverConnected);
        myInputChannel.responseReceiverDisconnected().subscribe(myOnResponseReceiverDisconnected);
        myInputChannel.messageReceived().subscribe(myOnMessageReceived);
    }
    
    public void clearTestResults()
    {
        myResponseReceiversAreConnectedEvent.reset();
        myAllResponseReceiversDisconnectedEvent.reset();
        myRequestMessagesReceivedEvent.reset();

        myConnectedResponseReceivers.clear();
        myDisconnectedResponseReceivers.clear();
        myReceivedMessages.clear();
    }
    
    public IDuplexInputChannel getInputChannel() { return myInputChannel; }
    
    public void waitUntilResponseReceiversConnectNotified(int numberOfExpectedReceivers, int milliseconds) throws Exception
    {
        synchronized (myConnectedResponseReceivers)
        {
            myNumberOfExpectedReceivers = numberOfExpectedReceivers;
            if (numberOfExpectedReceivers == myConnectedResponseReceivers.size())
            {
                myResponseReceiversAreConnectedEvent.set();
            }
            else
            {
                myResponseReceiversAreConnectedEvent.reset();
            }
        }

        EventWaitHandleExt.waitIfNotDebugging(myResponseReceiversAreConnectedEvent, milliseconds);
    }
    
    public void doOnResponseReceiverConnected(IMethod2<Object, ResponseReceiverEventArgs> doOnResponseReceiverConnected)
    {
        myDoOnResponseReceiverConnected = doOnResponseReceiverConnected;
    }
    
    public ArrayList<ResponseReceiverEventArgs> getConnectedResponseReceivers() { return myConnectedResponseReceivers; }
    
    public void waitUntilAllResponseReceiversDisconnectNotified(int milliseconds) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            EventWaitHandleExt.waitIfNotDebugging(myAllResponseReceiversDisconnectedEvent, milliseconds);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void waitUntilResponseRecieverIdDisconnectNotified(final String responseReceiverId, int milliseconds) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: it is correcto to use myConnectedResponseReceivers as lock and not myDisconnectedResponseReceivers.
            synchronized (myConnectedResponseReceivers)
            {
                if (EnumerableExt.any(myDisconnectedResponseReceivers, new IFunction1<Boolean, ResponseReceiverEventArgs>()
                    {
                        @Override
                        public Boolean invoke(ResponseReceiverEventArgs x)
                        {
                            return x.getResponseReceiverId().equals(responseReceiverId);
                        }
                    }))
                {
                    myExpectedResponseReceiverIdDisconnectedEvent.set();
                }
                else
                {
                    myExpectedDisconnectedResponseReceiverId = responseReceiverId;
                    myExpectedResponseReceiverIdDisconnectedEvent.reset();
                }
            }

            EventWaitHandleExt.waitIfNotDebugging(myExpectedResponseReceiverIdDisconnectedEvent, milliseconds);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public ArrayList<ResponseReceiverEventArgs> getDisconnectedResponseReceivers() { return myDisconnectedResponseReceivers; }
    
    public void waitUntilMessagesAreReceived(int numberOfExpectedMessages, int milliseconds) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myReceivedMessages)
            {
                myNumberOfExpectedRequestMessages = numberOfExpectedMessages;
                if (myReceivedMessages.size() == myNumberOfExpectedRequestMessages)
                {
                    myRequestMessagesReceivedEvent.set();
                }
                else
                {
                    myRequestMessagesReceivedEvent.reset();
                }
            }

            EventWaitHandleExt.waitIfNotDebugging(myRequestMessagesReceivedEvent, milliseconds);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void doOnMessageReceived(IMethod2<Object, DuplexChannelMessageEventArgs> doOnMessageReceived)
    {
        myDoOnMessageReceived = doOnMessageReceived;
    }
    
    public void doOnMessageReceived_SendResponse(final Object responseMessage)
    {
        myDoOnMessageReceived = new IMethod2<Object, DuplexChannelMessageEventArgs>()
        {
            @Override
            public void invoke(Object x, DuplexChannelMessageEventArgs y)
                    throws Exception
            {
                myInputChannel.sendResponseMessage(y.getResponseReceiverId(), responseMessage);
            }
        }; 
    }
    
    public ArrayList<DuplexChannelMessageEventArgs> getReceivedMessages() { return myReceivedMessages; } 
    
    private void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedResponseReceivers)
            {
                myConnectedResponseReceivers.add(e);

                if (myConnectedResponseReceivers.size() == myNumberOfExpectedReceivers)
                {
                    myResponseReceiversAreConnectedEvent.set();
                }

                if (myDoOnResponseReceiverConnected != null)
                {
                    try
                    {
                        myDoOnResponseReceiverConnected.invoke(sender, e);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error("Detected exception.", err);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseReceiverDisconnected(Object sender, final ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedResponseReceivers)
            {
                try
                {
                    ArrayListExt.removeAll(myConnectedResponseReceivers, new IFunction1<Boolean, ResponseReceiverEventArgs>()
                    {
                        @Override
                        public Boolean invoke(ResponseReceiverEventArgs x)
                                throws Exception
                        {
                            return x.getResponseReceiverId().equals(e.getResponseReceiverId());
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error("Removing connected response receivers failed.", err);
                }
                myDisconnectedResponseReceivers.add(e);

                if (myConnectedResponseReceivers.size() == 0)
                {
                    myAllResponseReceiversDisconnectedEvent.set();
                }

                if (e.getResponseReceiverId().equals(myExpectedDisconnectedResponseReceiverId))
                {
                    myExpectedResponseReceiverIdDisconnectedEvent.set();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myReceivedMessages)
            {
                myReceivedMessages.add(e);

                if (myReceivedMessages.size() == myNumberOfExpectedRequestMessages)
                {
                    myRequestMessagesReceivedEvent.set();
                }

                if (myDoOnMessageReceived != null)
                {
                    try
                    {
                        myDoOnMessageReceived.invoke(sender, e);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error("Detected exception.", err);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    private int myNumberOfExpectedReceivers;
    private ManualResetEvent myResponseReceiversAreConnectedEvent = new ManualResetEvent(false);
    private ArrayList<ResponseReceiverEventArgs> myConnectedResponseReceivers = new ArrayList<ResponseReceiverEventArgs>();
    private IMethod2<Object, ResponseReceiverEventArgs> myDoOnResponseReceiverConnected;

    private ManualResetEvent myAllResponseReceiversDisconnectedEvent = new ManualResetEvent(false);
    private ArrayList<ResponseReceiverEventArgs> myDisconnectedResponseReceivers = new ArrayList<ResponseReceiverEventArgs>();
    private ManualResetEvent myExpectedResponseReceiverIdDisconnectedEvent = new ManualResetEvent(false);
    private String myExpectedDisconnectedResponseReceiverId;

    private ManualResetEvent myRequestMessagesReceivedEvent = new ManualResetEvent(false);
    private ArrayList<DuplexChannelMessageEventArgs> myReceivedMessages = new ArrayList<DuplexChannelMessageEventArgs>();
    private int myNumberOfExpectedRequestMessages;
    private IMethod2<Object, DuplexChannelMessageEventArgs> myDoOnMessageReceived;
    
    private IDuplexInputChannel myInputChannel;
    
    private EventHandler<ResponseReceiverEventArgs> myOnResponseReceiverConnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverConnected(sender, e);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myOnResponseReceiverDisconnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverDisconnected(sender, e);
        }
    };
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onMessageReceived(sender, e);
        }
    };
}

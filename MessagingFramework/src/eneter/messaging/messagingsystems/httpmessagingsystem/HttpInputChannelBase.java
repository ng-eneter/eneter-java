package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.net.URI;

import eneter.messaging.dataprocessing.messagequeueing.WorkingThread;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.net.system.IMethod1;
import eneter.net.system.internal.StringExt;

abstract class HttpInputChannelBase
{
    public HttpInputChannelBase(String ipAddressAndPort, IServerSecurityFactory securityFactory)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(ipAddressAndPort))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }

            myChannelId = ipAddressAndPort;

            URI aUri;
            try
            {
                aUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            myListener = new HttpListener(aUri, securityFactory);

            myMessageProcessingThread = new WorkingThread<ProtocolMessage>(ipAddressAndPort);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public String getChannelId()
    {
        return myChannelId;
    }
    
    public void startListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                try
                {
                    // Start the working thread for removing messages from the queue
                    myMessageProcessingThread.registerMessageHandler(myHandleMessageHandler);

                    // Start listener.
                    myListener.startListening(myHandleConnectionHandler);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);

                    try
                    {
                        // Clear after failed start
                        stopListening();
                    }
                    catch (Exception err2)
                    {
                        // We tried to clean after failure. The exception can be ignored.
                    }

                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                // Stop the listener.
                myListener.stopListening();

                // Stop thread processing the queue with messages.
                try
                {
                    myMessageProcessingThread.unregisterMessageHandler();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.UnregisterMessageHandlerThreadFailure, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public boolean isListening() throws Exception
    { 
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                return myListener.isListening();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }

    }
    
    
    protected abstract void handleConnection(HttpRequestContext httpClientContext) throws Exception;
    
    protected abstract void handleMessage(ProtocolMessage protocolMessage);
    
    private String myChannelId;
    private Object myListeningManipulatorLock = new Object();
    private HttpListener myListener;
    protected WorkingThread<ProtocolMessage> myMessageProcessingThread;
    
    
    
    private IMethod1<HttpRequestContext> myHandleConnectionHandler = new IMethod1<HttpRequestContext>()
        {
            @Override
            public void invoke(HttpRequestContext t) throws Exception
            {
                handleConnection(t);
            }
        };
    
    private IMethod1<ProtocolMessage> myHandleMessageHandler = new IMethod1<ProtocolMessage>()
        {
            @Override
            public void invoke(ProtocolMessage t) throws Exception
            {
                handleMessage(t);
            }
        };
    
    protected abstract String TracedObject();
}

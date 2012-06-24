package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.URI;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.net.system.IMethod1;

public class WebSocketListener
{

    public WebSocketListener(URI webSocketUri)
    {
        this(webSocketUri, new NoneSecurityServerFactory());
    }
    
    public WebSocketListener(URI webSocketUri, IServerSecurityFactory securityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAddress = webSocketUri;
            mySecurityFactory = securityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void startListening(IMethod1<IWebSocketClientContext> connectionHandler) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
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

                    if (connectionHandler == null)
                    {
                        throw new IllegalArgumentException("The input parameter connectionHandler is null.");
                    }

                    myConnectionHandler = connectionHandler;

                    WebSocketListenerController.startListening(myAddress, myConnectionHandler, mySecurityFactory);
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                throw err;
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
            try
            {
                synchronized (myListeningManipulatorLock)
                {
                    WebSocketListenerController.stopListening(myAddress);
                    myConnectionHandler = null;
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.StartListeningFailure, err);
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
                return WebSocketListenerController.isListening(myAddress);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public URI getAddress()
    {
        return myAddress;
    }
    
    private URI myAddress;
    private IMethod1<IWebSocketClientContext> myConnectionHandler;
    private IServerSecurityFactory mySecurityFactory;
    private Object myListeningManipulatorLock = new Object();
    
    private String TracedObject()
    {
        return "WebSocketListener ";
    }
}

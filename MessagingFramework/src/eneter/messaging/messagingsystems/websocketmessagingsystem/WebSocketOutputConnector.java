/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.io.InputStream;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IClientSecurityFactory;
import eneter.net.system.*;

class WebSocketOutputConnector implements IOutputConnector
{
    public WebSocketOutputConnector(String inputConnectorAddress, String outputConnectorAddress, IProtocolFormatter protocolFormatter,
                        IClientSecurityFactory clientSecurityFactory,
                        int pingFrequency)
                    throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            URI aUri;
            try
            {
                aUri = new URI(inputConnectorAddress);
            }
            catch (Exception err)
            {
                EneterTrace.error(inputConnectorAddress + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            
            myClient = new WebSocketClient(aUri, clientSecurityFactory);
            
            myOutputConnectorAddress = outputConnectorAddress;
            myProtocolFormatter = protocolFormatter;
            
            myPingFrequency = pingFrequency;
            myTimer = new Timer("WebSocketOutputConnectorPinging", true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void openConnection(IMethod1<MessageContext> responseMessageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (responseMessageHandler == null)
            {
                throw new IllegalArgumentException("responseMessageHandler is null.");
            }
            
            synchronized (myConnectionManipulatorLock)
            {
                myResponseMessageHandler = responseMessageHandler;
                myClient.connectionClosed().subscribe(myOnWebSocketConnectionClosed);
                myClient.messageReceived().subscribe(myOnWebSocketMessageReceived);
    
                myClient.openConnection();
    
                myIpAddress = (myClient.getLocalEndPoint() != null) ? myClient.getLocalEndPoint().toString() : "";
                
                Object anEncodedMessage = myProtocolFormatter.encodeOpenConnectionMessage(myOutputConnectorAddress);
                if (anEncodedMessage != null)
                {
                    myClient.sendMessage(anEncodedMessage);
                }
                
                if (myPingFrequency > 0)
                {
                    myTimer.schedule(getTimerTask(), myPingFrequency);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: do not send a close message in WebSockets. Just close the socket.

            // Note: this must be before myClient.closeConnection().
            myResponseMessageHandler = null;
            
            myClient.closeConnection();
            myClient.messageReceived().unsubscribe(myOnWebSocketMessageReceived);
            myClient.connectionClosed().unsubscribe(myOnWebSocketConnectionClosed);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        return myClient.isConnected();
    }

    @Override
    public void sendRequestMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                Object anEncodedMessage = myProtocolFormatter.encodeMessage(myOutputConnectorAddress, message);
                myClient.sendMessage(anEncodedMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onWebSocketConnectionClosed(Object sender, Object e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IMethod1<MessageContext> aResponseHandler;
            synchronized (myConnectionManipulatorLock)
            {
                aResponseHandler = myResponseMessageHandler;
                closeConnection();
            }

            ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, myOutputConnectorAddress, null);
            MessageContext aMessageContext = new MessageContext(aProtocolMessage, myIpAddress);

            try
            {
                // If the connection closed is not caused that the client called CloseConnection()
                // but the connection was closed from the service.
                if (aResponseHandler != null)
                {
                    aResponseHandler.invoke(aMessageContext);
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onWebSocketMessageReceived(Object sender, WebSocketMessage e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage((InputStream)e.getInputStream());
            MessageContext aMessageContext = new MessageContext(aProtocolMessage, myIpAddress);

            try
            {
                myResponseMessageHandler.invoke(aMessageContext);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onPing()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myClient.sendPing();
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "failed to send the ping.", err);
            }
            
            // If the connection is open then schedule the next ping.
            if (isConnected())
            {
                myTimer.schedule(getTimerTask(), myPingFrequency);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /*
     * Helper method to get the new instance of the timer task.
     * The problem is, the timer does not allow to reschedule the same instance of the TimerTask
     * and the exception is thrown.
     */
    private TimerTask getTimerTask()
    {
        TimerTask aTimerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                onPing();
            }
        };
        
        return aTimerTask;
    }
    
    private String myOutputConnectorAddress;
    private IProtocolFormatter myProtocolFormatter;
    private WebSocketClient myClient;
    private IMethod1<MessageContext> myResponseMessageHandler;
    private String myIpAddress;
    private Object myConnectionManipulatorLock = new Object();
    private Timer myTimer;
    private int myPingFrequency;

    
    private EventHandler<WebSocketMessage> myOnWebSocketMessageReceived = new EventHandler<WebSocketMessage>()
    {
        @Override
        public void onEvent(Object sender, WebSocketMessage e)
        {
            onWebSocketMessageReceived(sender, e);
        }
    };
    
    private EventHandler<Object> myOnWebSocketConnectionClosed = new EventHandler<Object>()
    {
        @Override
        public void onEvent(Object sender, Object e)
        {
            onWebSocketConnectionClosed(sender, e);
        }
    };
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.io.OutputStream;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IClientSecurityFactory;
import eneter.net.system.*;

class WebSocketOutputConnector implements IOutputConnector
{
    public WebSocketOutputConnector(String serviceConnectorAddress,
            int pingFrequency,
            IClientSecurityFactory clientSecurityFactory)
                    throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            URI aUri;
            try
            {
                aUri = new URI(serviceConnectorAddress);
            }
            catch (Exception err)
            {
                EneterTrace.error(serviceConnectorAddress + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            
            myClient = new WebSocketClient(aUri, clientSecurityFactory);
            
            myPingFrequency = pingFrequency;
            myTimer = new Timer("ReliableMessageTimeTracker", true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void openConnection(
            IFunction1<Boolean, MessageContext> responseMessageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (responseMessageHandler != null)
            {
                myClient.connectionClosed().subscribe(myOnWebSocketConnectionClosed);
                myClient.messageReceived().subscribe(myOnWebSocketMessageReceived);
                myResponseMessageHandler = responseMessageHandler;
            }

            myClient.openConnection();

            myIpAddress = (myClient.getLocalEndPoint() != null) ? myClient.getLocalEndPoint().toString() : "";
            
            if (myPingFrequency > 0)
            {
                myTimer.schedule(getTimerTask(), myPingFrequency);
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
    public boolean isStreamWritter()
    {
        return false;
    }

    @Override
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myClient.sendMessage(message);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void sendMessage(IMethod1<OutputStream> toStreamWritter)
            throws Exception
    {
        throw new UnsupportedOperationException();
    }
    
    private void onWebSocketConnectionClosed(Object sender, Object e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseMessageHandler != null)
            {
                // With null indicate the connection was closed.
                myResponseMessageHandler.invoke(null);
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
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
            if (myResponseMessageHandler != null)
            {
                MessageContext aContext = new MessageContext(e.getInputStream(), myIpAddress, this);
                myResponseMessageHandler.invoke(aContext);
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onTimerTick()
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
                onTimerTick();
            }
        };
        
        return aTimerTask;
    }
    
    private WebSocketClient myClient;
    private IFunction1<Boolean, MessageContext> myResponseMessageHandler;
    private String myIpAddress;
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
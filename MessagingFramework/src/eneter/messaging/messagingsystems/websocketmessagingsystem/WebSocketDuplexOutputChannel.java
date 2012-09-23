/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IClientSecurityFactory;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;
import eneter.net.system.threading.internal.ThreadPool;

class WebSocketDuplexOutputChannel implements IDuplexOutputChannel
{
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpenedEvent.getApi();
    }

    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosedEvent.getApi();
    }

    public Event<DuplexChannelMessageEventArgs> responseMessageReceived()
    {
        return myResponseMessageReceivedEvent.getApi();
    }
    
    
    
    public WebSocketDuplexOutputChannel(String ipAddressAndPort, String responseReceiverId,
            long pingFrequency,
            IClientSecurityFactory securityStreamFactory, IProtocolFormatter<?> protocolFormatter)
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

            URI aUri;
            try
            {
                // just check if the address is valid
                aUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            myClient = new WebSocketClient(aUri, securityStreamFactory);
            myClient.connectionClosed().subscribe(myOnWebSocketConnectionClosed);
            myClient.messageReceived().subscribe(myOnWebSocketMessageReceived);

            myChannelId = ipAddressAndPort;

            myResponseReceiverId = (StringExt.isNullOrEmpty(responseReceiverId)) ? ipAddressAndPort + "_" + UUID.randomUUID().toString() : responseReceiverId;

            myProtocolFormatter = protocolFormatter;
            
            myPingFrequency = pingFrequency;
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
    
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }
    
    public void openConnection() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (isConnected())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyConnected;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                // If it is needed clear after previous connection
                if (myClient != null)
                {
                    try
                    {
                        closeConnection();
                    }
                    catch (Exception err)
                    {
                        // We tried to clean after the previous connection. The exception can be ignored.
                    }
                }

                try
                {
                    // Open WebSocket connection.
                    myClient.openConnection();

                    // Encode the request to open the connection.
                    Object anEncodedMessage = myProtocolFormatter.encodeOpenConnectionMessage(getResponseReceiverId());

                    // Send the message.
                    myClient.sendMessage(anEncodedMessage);
                    
                    // Set the timer to send pings with desired frequency.
                    if (myPingFrequency > 0)
                    {
                        myPingTimer = new Timer("WebsocketPingTimer", true);
                        myPingTimer.schedule(getTimerTask(), 0, myPingFrequency);
                    }
                    
                    // Invoke the event notifying, the connection was opened.
                    notifyConnectionOpened();
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.OpenConnectionFailure, err);

                    try
                    {
                        closeConnection();
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
    
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                // Cancel pinging.
                if (myPingTimer != null)
                {
                    myPingTimer.cancel();
                    myPingTimer.purge();
                    myPingTimer = null;
                }
                
                if (myClient != null && myClient.isConnected())
                {
                    // Try to notify that the connection is closed
                    try
                    {
                        // Encode the message to close the connection.
                        Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(getResponseReceiverId());

                        // Send the message.
                        myClient.sendMessage(anEncodedMessage);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                    }

                    try
                    {
                        // This will close the connection with the server.
                        myClient.closeConnection();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to close Tcp connection.", err);
                    }
                }

            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public boolean isConnected()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                return myClient != null && myClient.isConnected();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (!isConnected())
                {
                    String aMessage = TracedObject() + ErrorHandler.SendMessageNotConnectedFailure;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                try
                {
                    // Encode the message.
                    Object anEncodedMessage = myProtocolFormatter.encodeMessage(getResponseReceiverId(), message);

                    // Send the message.
                    myClient.sendMessage(anEncodedMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                    throw err;
                }
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
            try
            {
                // Decode the incoming message.
                ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(e.getInputStream());

                if (aProtocolMessage != null)
                {
                    if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                    {
                        if (myResponseMessageReceivedEvent.isSubscribed())
                        {
                            try
                            {
                                DuplexChannelMessageEventArgs anEvent = new DuplexChannelMessageEventArgs(getChannelId(), aProtocolMessage.Message, getResponseReceiverId());
                                myResponseMessageReceivedEvent.raise(this, anEvent);
                            }
                            catch (Exception err)
                            {
                                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                            }
                        }
                        else
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
                        }
                    }
                    else
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
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
            try
            {
                if (myConnectionClosedEvent.isSubscribed())
                {
                    DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId());
                    myConnectionClosedEvent.raise(this, aMsg);
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
    
    private void notifyConnectionOpened()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ThreadPool.queueUserWorkItem(new Runnable()
            {
                @Override
                public void run()
                {
                    EneterTrace aTrace = EneterTrace.entering();
                    try
                    {
                        try
                        {
                            if (myConnectionOpenedEvent.isSubscribed())
                            {
                                DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId());
                                myConnectionOpenedEvent.raise(this, aMsg);
                            }
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                        }
                        catch (Error err)
                        {
                            EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                            throw err;
                        }
                    }
                    finally
                    {
                        EneterTrace.leaving(aTrace);
                    }
                }
            });
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    /**
     * This is intended for mobile devices to keep the connection open.
     * Note: The problem is, if the connection is not active longer time,
     *       the TCP connection is dropped.
     */
    private void onPingTick()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // We need jsut to keep the connection open.
            // So if we send the pong instead of ping, then the server will not send
            // back any response but it is ok - we do not need it.
            myClient.sendPong();
        }
        catch (Exception err)
        {
            EneterTrace.warning(TracedObject() + "failed to send pong.", err);
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
                onPingTick();
            }
        };
        
        return aTimerTask;
    }

    
    
    private String myChannelId;
    private String myResponseReceiverId;
    
    private WebSocketClient myClient;
    private Timer myPingTimer;
    private long myPingFrequency;
    private Object myConnectionManipulatorLock = new Object();
    
    private IProtocolFormatter<?> myProtocolFormatter;
    
    
    private EventHandler<Object> myOnWebSocketConnectionClosed = new EventHandler<Object>()
    {
        @Override
        public void onEvent(Object sender, Object e)
        {
            onWebSocketConnectionClosed(sender, e);
        }
    };
    
    private EventHandler<WebSocketMessage> myOnWebSocketMessageReceived = new EventHandler<WebSocketMessage>()
    {
        @Override
        public void onEvent(Object sender, WebSocketMessage e)
        {
            onWebSocketMessageReceived(sender, e);
        }
    };
    
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEvent = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEvent = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEvent = new EventImpl<DuplexChannelMessageEventArgs>();
    
    
    private String TracedObject()
    {
        return "WebSocket duplex output channel '" + getChannelId() + "' ";
    }
}

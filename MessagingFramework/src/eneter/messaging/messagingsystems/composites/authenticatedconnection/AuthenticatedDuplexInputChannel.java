/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

import java.util.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;



class AuthenticatedDuplexInputChannel implements IDuplexInputChannel
{
    private class TNotYetAuthenticatedConnection
    {
        public Object LoginMessage;
        public Object HandshakeMessage;
    }
    

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEventImpl.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEventImpl.getApi();
    }

    @Override
    public Event<DuplexChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventImpl.getApi();
    }

    
    public AuthenticatedDuplexInputChannel(IDuplexInputChannel underlyingInputChannel,
            IGetHandshakeMessage getHandshakeMessageCallback,
            IAuthenticate verifyHandshakeResponseMessageCallback,
            IHandleAuthenticationCancelled authenticationCancelledCallback)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
            {
                myUnderlayingInputChannel = underlyingInputChannel;
                myGetHandshakeMessageCallback = getHandshakeMessageCallback;
                myAuthenticateCallback = verifyHandshakeResponseMessageCallback;
                myAuthenticationCancelledCallback = authenticationCancelledCallback;

                myUnderlayingInputChannel.responseReceiverDisconnected().subscribe(myOnResponseReceiverDisconnected);
                myUnderlayingInputChannel.messageReceived().subscribe(myOnMessageReceived);
            }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public String getChannelId()
    {
        return myUnderlayingInputChannel.getChannelId();
    }
    
    @Override
    public void startListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlayingInputChannel.startListening();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlayingInputChannel.stopListening();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening()
    {
        return myUnderlayingInputChannel.isListening();
    }
    
    @Override
    public IThreadDispatcher getDispatcher()
    {
        return myUnderlayingInputChannel.getDispatcher();
    }

    @Override
    public void sendResponseMessage(String responseReceiverId, Object message)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (!isListening())
            {
                String aMessage = TracedObject() + ErrorHandler.FailedToSendResponseBecauseNotListening;
                EneterTrace.error(aMessage);
                throw new IllegalStateException(aMessage);
            }
            
            if (responseReceiverId.equals("*"))
            {
                myAuthenticatedConnectionsLock.lock();
                try
                {
                    // Send the response message to all connected clients.
                    for (String aConnectedClient : myAuthenticatedConnections)
                    {
                        try
                        {
                            // Send the response message.
                            myUnderlayingInputChannel.sendResponseMessage(aConnectedClient, message);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error(TracedObject() + ErrorHandler.FailedToSendResponseMessage, err);

                            // Note: Exception is not rethrown because if sending to one client fails it should not
                            //       affect sending to other clients.
                        }
                    }
                }
                finally
                {
                    myAuthenticatedConnectionsLock.unlock();
                }
            }
            else
            {
                myAuthenticatedConnectionsLock.lock();
                try
                {
                    if (!myAuthenticatedConnections.contains(responseReceiverId))
                    {
                        String aMessage = TracedObject() + ErrorHandler.FailedToSendResponseBecauaeClientNotConnected;
                        EneterTrace.error(aMessage);
                        throw new IllegalStateException(aMessage);
                    }
                }
                finally
                {
                    myAuthenticatedConnectionsLock.unlock();
                }

                myUnderlayingInputChannel.sendResponseMessage(responseReceiverId, message);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void disconnectResponseReceiver(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAuthenticatedConnectionsLock.lock();
            try
            {
                myAuthenticatedConnections.remove(responseReceiverId);
            }
            finally
            {
                myAuthenticatedConnectionsLock.unlock();
            }

            myNotYetAuthenticatedConnectionsLock.lock();
            try
            {
                myNotYetAuthenticatedConnections.remove(responseReceiverId);
            }
            finally
            {
                myNotYetAuthenticatedConnectionsLock.unlock();
            }
            
            myUnderlayingInputChannel.disconnectResponseReceiver(responseReceiverId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TNotYetAuthenticatedConnection aNotAuthenticatedConnection;
            myNotYetAuthenticatedConnectionsLock.lock();
            try
            {
                aNotAuthenticatedConnection = myNotYetAuthenticatedConnections.get(e.getResponseReceiverId());
                if (aNotAuthenticatedConnection != null)
                {
                    myNotYetAuthenticatedConnections.remove(e.getResponseReceiverId());
                }
            }
            finally
            {
                myNotYetAuthenticatedConnectionsLock.unlock();
            }
            if (aNotAuthenticatedConnection != null && myAuthenticationCancelledCallback != null)
            {
                try
                {
                    myAuthenticationCancelledCallback.handleAuthenticationCancelled(getChannelId(), e.getResponseReceiverId(), aNotAuthenticatedConnection.LoginMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
            
            boolean anIsAuthenticatedConnection;
            myAuthenticatedConnectionsLock.lock();
            try
            {
                anIsAuthenticatedConnection = myAuthenticatedConnections.remove(e.getResponseReceiverId());
            }
            finally
            {
                myAuthenticatedConnectionsLock.unlock();
            }

            if (anIsAuthenticatedConnection)
            {
                notifyEvent(myResponseReceiverDisconnectedEventImpl, e, false);
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
            // If the connection has already been authenticated then this is a regular request message that will be notified.
            boolean anIsAuthenticated;
            myAuthenticatedConnectionsLock.lock();
            try
            {
                anIsAuthenticated = myAuthenticatedConnections.contains(e.getResponseReceiverId());
            }
            finally
            {
                myAuthenticatedConnectionsLock.unlock();
            }
            
            if (anIsAuthenticated)
            {
                notifyEvent(myMessageReceivedEventImpl, e, true);
                return;
            }


            boolean aDisconnectFlag = true;
            boolean aNewResponseReceiverAuthenticated = false;

            TNotYetAuthenticatedConnection aConnection;
            myNotYetAuthenticatedConnectionsLock.lock();
            try
            {
                aConnection = myNotYetAuthenticatedConnections.get(e.getResponseReceiverId());
                if (aConnection == null)
                {
                    aConnection = new TNotYetAuthenticatedConnection();
                    myNotYetAuthenticatedConnections.put(e.getResponseReceiverId(), aConnection);
                }
            }
            finally
            {
                myNotYetAuthenticatedConnectionsLock.unlock();
            }
            
            if (aConnection.HandshakeMessage == null)
            {
                EneterTrace.debug("LOGIN RECEIVED");

                // If the connection is in the state that it is not logged in then this must be the login message.
                // The handshake message will be sent.
                try
                {
                    aConnection.LoginMessage = e.getMessage();
                    aConnection.HandshakeMessage = myGetHandshakeMessageCallback.getHandshakeMessage(e.getChannelId(), e.getResponseReceiverId(), e.getMessage());

                    // If the login was accepted.
                    if (aConnection.HandshakeMessage != null)
                    {
                        try
                        {
                            // Send the handshake message to the client.
                            myUnderlayingInputChannel.sendResponseMessage(e.getResponseReceiverId(), aConnection.HandshakeMessage);
                            aDisconnectFlag = false;
                        }
                        catch (Exception err)
                        {
                            String anErrorMessage = TracedObject() + "failed to send the handshake message. The client will be disconnected.";
                            EneterTrace.error(anErrorMessage, err);
                        }
                    }
                    else
                    {
                        // the client will be disconnected.
                    }
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + "failed to get the handshake message. The client will be disconnected.";
                    EneterTrace.error(anErrorMessage, err);
                }
            }
            else
            // If the connection is in the state that the handshake message was sent then this is the handshake response message.
            // The response for the handshake will be verified.
            {
                EneterTrace.debug("HANDSHAKE RESPONSE RECEIVED");
                
                try
                {
                    if (myAuthenticateCallback.authenticate(e.getChannelId(), e.getResponseReceiverId(), aConnection.LoginMessage, aConnection.HandshakeMessage, e.getMessage()))
                    {
                        // Send acknowledge message that the connection is authenticated.
                        try
                        {
                            myUnderlayingInputChannel.sendResponseMessage(e.getResponseReceiverId(), "OK");

                            aDisconnectFlag = false;
                            aNewResponseReceiverAuthenticated = true;

                            // Move the connection from not-yet-authenticated to authenticated connections.
                            myAuthenticatedConnectionsLock.lock();
                            try
                            {
                                myAuthenticatedConnections.add(e.getResponseReceiverId());
                            }
                            finally
                            {
                                myAuthenticatedConnectionsLock.unlock();
                            }
                            
                            myNotYetAuthenticatedConnectionsLock.lock();
                            try
                            {
                                myNotYetAuthenticatedConnections.remove(e.getResponseReceiverId());
                            }
                            finally
                            {
                                myNotYetAuthenticatedConnectionsLock.unlock();
                            }
                        }
                        catch (Exception err)
                        {
                            String anErrorMessage = TracedObject() + "failed to send the acknowledge message that the connection was authenticated. The client will be disconnected.";
                            EneterTrace.error(anErrorMessage, err);
                        }
                    }
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + "failed to verify the response for the handshake message. The client will be disconnected.";
                    EneterTrace.error(anErrorMessage, err);
                }
            }
                

            // If the connection with the client shall be closed.
            if (aDisconnectFlag)
            {
                myNotYetAuthenticatedConnectionsLock.lock();
                try
                {
                    myNotYetAuthenticatedConnections.remove(e.getResponseReceiverId());
                }
                finally
                {
                    myNotYetAuthenticatedConnectionsLock.unlock();
                }
                
                try
                {
                    myUnderlayingInputChannel.disconnectResponseReceiver(e.getResponseReceiverId());
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + "failed to disconnect response receiver.";
                    EneterTrace.warning(anErrorMessage, err);
                }
            }

            // Notify ResponseReceiverConnected if a new connection is authenticated.
            // Note: the notification runs outside the lock in order to reduce blocking.
            if (aNewResponseReceiverAuthenticated)
            {
                ResponseReceiverEventArgs anEventArgs = new ResponseReceiverEventArgs(e.getResponseReceiverId(), e.getSenderAddress());
                notifyEvent(myResponseReceiverConnectedEventImpl, anEventArgs, false);
            }

        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private <T> void notifyEvent(EventImpl<T> handler, T event, boolean isNobodySubscribedWarning)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler != null)
            {
                try
                {
                    handler.raise(this, event);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
            else if (isNobodySubscribedWarning)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IDuplexInputChannel myUnderlayingInputChannel;
    
    private ThreadLock myNotYetAuthenticatedConnectionsLock = new ThreadLock();
    private HashMap<String, TNotYetAuthenticatedConnection> myNotYetAuthenticatedConnections = new HashMap<String, TNotYetAuthenticatedConnection>();
    private ThreadLock myAuthenticatedConnectionsLock = new ThreadLock();
    private HashSet<String> myAuthenticatedConnections = new HashSet<String>();
    
    private IGetHandshakeMessage myGetHandshakeMessageCallback;
    private IAuthenticate myAuthenticateCallback;
    private IHandleAuthenticationCancelled myAuthenticationCancelledCallback;
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    
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
    
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " '" + getChannelId() + "' ";
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.*;
import java.net.URI;
import java.util.*;


import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;
import eneter.net.system.internal.*;
import eneter.net.system.linq.internal.EnumerableExt;

class HttpInputConnector implements IInputConnector
{
    private class HttpResponseSender implements IDisposable
    {
        public HttpResponseSender(String responseReceiverId, String clientIp)
        {
            ResponseReceiverId = responseReceiverId;
            ClientIp = clientIp;
            LastPollingActivityTime = System.currentTimeMillis();
        }

        // Note: This dispose is called when the duplex input channel disconnected the client.
        //       However, in HTTP messaging the client gets messages using the polling.
        //       So, although the service disconnected the client there can be still messages
        //       in the queue waiting for the polling.
        //       Therefore these messages must be still available after the dispose.
        @Override
        public void dispose()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                IsDisposed = true;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public void sendResponseMessage(Object message) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                if (IsDisposed)
                {
                    throw new IllegalStateException(getClass().getSimpleName() + " is disposed.");
                }

                synchronized (myMessages)
                {
                    byte[] aMessage = (byte[])message;
                    myMessages.add(aMessage);
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        
        // Note: this method must be available after the dispose.
        public byte[] dequeueCollectedMessages()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                byte[] aDequedMessages = null;

                synchronized (myMessages)
                {
                    // Update the polling time.
                    LastPollingActivityTime = System.currentTimeMillis();

                    // If there are stored messages for the receiver
                    if (myMessages.size() > 0)
                    {
                        int aSizeOfResponseMessages = 0;
                        ByteArrayOutputStream aStreamedResponses = new ByteArrayOutputStream();

                        // Dequeue responses to be sent to the response receiver.
                        // Note: Try not to exceed 1MB - better do more small transfers
                        while (myMessages.size() > 0 && aSizeOfResponseMessages < 1048576)
                        {
                            // Get the response message formatted according to the connection protocol.
                            byte[] aResponseMessage = myMessages.poll();
                            aStreamedResponses.write(aResponseMessage, 0, aResponseMessage.length);
                            
                            aSizeOfResponseMessages += aResponseMessage.length;
                        }

                        aDequedMessages = aStreamedResponses.toByteArray();
                    }
                }

                return aDequedMessages;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        public String ResponseReceiverId;
        public String ClientIp;
        public long LastPollingActivityTime;
        public boolean IsDisposed;
        
        private Queue<byte[]> myMessages = new ArrayDeque<byte[]>();
    }
    

    
    public HttpInputConnector(String httpAddress, IProtocolFormatter protocolFormatter, int responseReceiverInactivityTimeout,
            IServerSecurityFactory serverSecurityFactory) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            URI aUri;
            try
            {
                aUri = new URI(httpAddress);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            myHttpListenerProvider = new HttpListener(aUri, serverSecurityFactory);

            myProtocolFormatter = protocolFormatter;
            myResponseReceiverInactivityTimeout = responseReceiverInactivityTimeout;


            // Initialize the timer to regularly check the timeout for connections with duplex output channels.
            // If the duplex output channel did not poll within the timeout then the connection
            // is closed and removed from the list.
            // Note: The timer is set here but not executed.
            myResponseReceiverInactivityTimer = new Timer(true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void startListening(IMethod1<MessageContext> messageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (messageHandler == null)
            {
                throw new IllegalArgumentException("messageHandler is null.");
            }
            
            synchronized (myListeningManipulatorLock)
            {
                try
                {
                    myMessageHandler = messageHandler;
                    myHttpListenerProvider.startListening(myHandleConnection);
                }
                catch (Exception err)
                {
                    stopListening();
                    throw err;
                }
            }
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
            synchronized (myListeningManipulatorLock)
            {
                myHttpListenerProvider.stopListening();
                myMessageHandler = null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            return myHttpListenerProvider.isListening();
        }
    }

    @Override
    public void sendResponseMessage(final String outputConnectorAddress, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            HttpResponseSender aClientContext;
            synchronized (myConnectedClients)
            {
                aClientContext = EnumerableExt.firstOrDefault(myConnectedClients, new IFunction1<Boolean, HttpResponseSender>()
                {
                    @Override
                    public Boolean invoke(HttpResponseSender x)
                            throws Exception
                    {
                        return x.ResponseReceiverId.equals(outputConnectorAddress);
                    }
                });
            }

            if (aClientContext == null)
            {
                throw new IllegalStateException("The connection with client '" + outputConnectorAddress + "' is not open.");
            }

            Object anEncodedMessage = myProtocolFormatter.encodeMessage(outputConnectorAddress, message);
            aClientContext.sendResponseMessage(anEncodedMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void closeConnection(final String outputConnectorAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            HttpResponseSender aClientContext;
            synchronized (myConnectedClients)
            {
                aClientContext = EnumerableExt.firstOrDefault(myConnectedClients, new IFunction1<Boolean, HttpResponseSender>()
                {
                    @Override
                    public Boolean invoke(HttpResponseSender x)
                            throws Exception
                    {
                        return x.ResponseReceiverId.equals(outputConnectorAddress);
                    }
                }); 
                        
                // Note: we cannot remove the client context from myConnectedClients because the following close message
                //       will be put to the queue. And if it is removed from myConnectedClients then the client context
                //       would not be found during polling and the close connection message woiuld never be sent to the client.
                //       
                //       The removing of the client context works like this:
                //       The client gets the close connection message. The client processes it and stops polling.
                //       On the service side the time detects the client sopped polling and so it removes
                //       the client context from my connected clients.
            }

            if (aClientContext != null)
            {
                try
                {
                    // Send close connection message.
                    Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(outputConnectorAddress);
                    aClientContext.sendResponseMessage(anEncodedMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.warning("failed to send the close message.", err);
                }

                aClientContext.dispose();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


   
    
    private void handleConnection(HttpRequestContext httpRequestContext) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // If polling. (when client polls to get response messages)
            if (httpRequestContext.getHttpMethod().equals("GET"))
            {
                // Get responseReceiverId.
                String aQuery = httpRequestContext.getUri().getQuery();
                if (!StringExt.isNullOrEmpty(aQuery))
                {
                    final String aResponseReceiverId = aQuery.substring(3);

                    // Find the sender for the response receiver.
                    HttpResponseSender aResponseSender = null;
                    synchronized (myConnectedClients)
                    {
                        aResponseSender = EnumerableExt.firstOrDefault(myConnectedClients, new IFunction1<Boolean, HttpResponseSender>()
                        {
                            @Override
                            public Boolean invoke(HttpResponseSender x) throws Exception
                            {
                                return x.ResponseReceiverId.equals(aResponseReceiverId);
                            }
                    
                        });
                    }

                    if (aResponseSender != null)
                    {
                        // Response collected messages.
                        byte[] aMessages = aResponseSender.dequeueCollectedMessages();
                        if (aMessages != null)
                        {
                            httpRequestContext.response(aMessages);
                        }
                    }
                    else
                    {
                        // Note: This happens when the polling runs and the connection is not open yet.
                        //       It is a normal situation because the polling thread on the client starts
                        //       slightly before the connection is open.
                    }
                }
                else
                {
                    EneterTrace.warning("Incorrect query format detected for HTTP GET request.");

                    // The request was not processed.
                    httpRequestContext.responseError(404);
                }
            }
            else
             // Client sends a request message.
            {
                byte[] aMessage = httpRequestContext.getRequestMessage();

                String aClientIp = httpRequestContext.getRemoteEndPoint();
                
                final ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(aMessage);
                
                boolean anIsProcessingOk = true;
                if (aProtocolMessage != null && !StringExt.isNullOrEmpty(aProtocolMessage.ResponseReceiverId))
                {
                    MessageContext aMessageContext = new MessageContext(aProtocolMessage, aClientIp);

                    if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
                    {
                        synchronized (myConnectedClients)
                        {
                            HttpResponseSender aClientContext = EnumerableExt.firstOrDefault(myConnectedClients,
                                new IFunction1<Boolean, HttpResponseSender>()
                                {
                                    @Override
                                    public Boolean invoke(HttpResponseSender x) throws Exception
                                    {
                                        return x.ResponseReceiverId.equals(aProtocolMessage.ResponseReceiverId); 
                                    }
                                }
                            );
                                    
                            if (aClientContext != null && aClientContext.IsDisposed)
                            {
                                // The client with the same id exists but was closed and disposed.
                                // It is just that the timer did not remove it. So delete it now.
                                myConnectedClients.remove(aClientContext);
                                
                                // Indicate the new client context shall be created.
                                aClientContext = null;
                            }

                            if (aClientContext == null)
                            {
                                aClientContext = new HttpResponseSender(aProtocolMessage.ResponseReceiverId, aClientIp);
                                myConnectedClients.add(aClientContext);

                                // If this is the only sender then start the timer measuring the inactivity to detect if the client is disconnected.
                                // If it is not the only sender, then the timer is already running.
                                if (myConnectedClients.size() == 1)
                                {
                                    myResponseReceiverInactivityTimer.schedule(getTimerTask(), myResponseReceiverInactivityTimeout);
                                }
                            }
                            else
                            {
                                EneterTrace.warning(TracedObject() + "could not open connection for client '" + aProtocolMessage.ResponseReceiverId + "' because the client with same id is already connected.");
                                anIsProcessingOk = false;
                            }
                        }
                    }
                    else if (aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
                    {
                        synchronized (myConnectedClients)
                        {
                            HttpResponseSender aClientContext = EnumerableExt.firstOrDefault(myConnectedClients, new IFunction1<Boolean, HttpResponseSender>()
                            {
                                @Override
                                public Boolean invoke(HttpResponseSender x) throws Exception
                                {
                                    return x.ResponseReceiverId.equals(aProtocolMessage.ResponseReceiverId);
                                }
                            });
                                    
                            if (aClientContext != null)
                            {
                                // Note: the disconnection comes from the client.
                                //       It means the client closed the connection and will not poll anymore.
                                //       Therefore the client context can be removed.
                                myConnectedClients.remove(aClientContext);
                                aClientContext.dispose();
                            }
                        }
                    }
                    
                    if (anIsProcessingOk)
                    {
                        try
                        {
                            myMessageHandler.invoke(aMessageContext);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                        }
                    }
                }
                
                if (!anIsProcessingOk)
                {
                    // The request was not processed.
                    httpRequestContext.responseError(404);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onConnectionCheckTimer()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            final ArrayList<HttpResponseSender> aClientsToNotify = new ArrayList<HttpResponseSender>();
            boolean aStartTimerFlag = false;
            
            synchronized (myConnectedClients)
            {
                final long aTime = System.currentTimeMillis();

                // Check the connection for each connected duplex output channel.
                HashSetExt.removeWhere(myConnectedClients, new IFunction1<Boolean, HttpResponseSender>()
                {
                    @Override
                    public Boolean invoke(HttpResponseSender x) throws Exception
                    {
                        // If the last polling activity time exceeded the maximum allowed time then
                        // it is considered the connection is closed.
                        // Note: it must be >= because the 1st HttpResponseSender is created at the same time as the timer is executed.
                        //       it caused problems with 'Inactivity_Timeout' unit test.
                        if (aTime - x.LastPollingActivityTime >= myResponseReceiverInactivityTimeout)
                        {
                            // If the connection was broken unexpectedly then the message handler must be notified.
                            if (!x.IsDisposed)
                            {
                                aClientsToNotify.add(x);
                            }

                            // Indicate to remove the item.
                            return true;
                        }

                        // Indicate to keep the item.
                        return false;
                    }
                }); 

                // If there connected clients we need to check if they are active.
                if (myConnectedClients.size() > 0)
                {
                    aStartTimerFlag = true;
                }
            }
            
            for (HttpResponseSender aClientContext : aClientsToNotify)
            {
                ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, aClientContext.ResponseReceiverId, null);
                MessageContext aMessageContext = new MessageContext(aProtocolMessage, aClientContext.ClientIp);

                try
                {
                    if (myMessageHandler != null)
                    {
                        myMessageHandler.invoke(aMessageContext);
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
            
            if (aStartTimerFlag)
            {
                myResponseReceiverInactivityTimer.schedule(getTimerTask(), myResponseReceiverInactivityTimeout);
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "failed to check timeouted clients.", err);
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
                try
                {
                    onConnectionCheckTimer();
                }
                catch (Exception e)
                {
                }
            }
        };
        
        return aTimerTask;
    }
    
    
    private IProtocolFormatter myProtocolFormatter;
    private HttpListener myHttpListenerProvider;
    private IMethod1<MessageContext> myMessageHandler;
    private Object myListeningManipulatorLock = new Object();
    private Timer myResponseReceiverInactivityTimer;
    private int myResponseReceiverInactivityTimeout;
    private HashSet<HttpResponseSender> myConnectedClients = new HashSet<HttpResponseSender>();
    
    private IMethod1<HttpRequestContext> myHandleConnection = new IMethod1<HttpRequestContext>()
    {
        @Override
        public void invoke(HttpRequestContext x) throws Exception
        {
            handleConnection(x);
        }
    };
    
    
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

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
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;
import eneter.net.system.internal.IDisposable;
import eneter.net.system.linq.internal.EnumerableExt;

class HttpInputConnector implements IInputConnector
{
    private class HttpResponseSender implements ISender, IDisposable
    {
        public HttpResponseSender(String responseReceiverId)
        {
            ResponseReceiverId = responseReceiverId;
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

        @Override
        public void sendMessage(IMethod1<OutputStream> toStreamWritter)
                throws Exception
        {
            throw new UnsupportedOperationException("Http ResponseSender is not a stream sender.");
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
        public long LastPollingActivityTime;
        public boolean IsDisposed;
        
        private Queue<byte[]> myMessages = new ArrayDeque<byte[]>();
    }
    

    
    public HttpInputConnector(String httpAddress, int responseReceiverInactivityTimeout) throws Exception
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
            
            myHttpListenerProvider = new HttpListener(aUri);

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
    public void startListening(
            IFunction1<Boolean, MessageContext> messageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessageHandler = messageHandler;
            myHttpListenerProvider.startListening(myHandleConnection);
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
            myHttpListenerProvider.stopListening();
            myMessageHandler = null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening() throws Exception
    {
        return myHttpListenerProvider.isListening();
    }

    @Override
    public ISender createResponseSender(final String responseReceiverAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myResponseSenders)
            {
                // If there are some disposed senders then remove them.
                HashSetExt.removeWhere(myResponseSenders, new IFunction1<Boolean, HttpResponseSender>()
                {
                    @Override
                    public Boolean invoke(HttpResponseSender x)
                            throws Exception
                    {
                        return x.IsDisposed;
                    }
                });
                
                // If does not exist create one.
                HttpResponseSender aResponseSender = EnumerableExt.firstOrDefault(myResponseSenders, new IFunction1<Boolean, HttpResponseSender>()
                {
                    @Override
                    public Boolean invoke(HttpResponseSender x)
                            throws Exception
                    {
                        return x.ResponseReceiverId.equals(responseReceiverAddress);
                    }
                });
                
                if (aResponseSender == null)
                {
                    aResponseSender = new HttpResponseSender(responseReceiverAddress);
                    myResponseSenders.add(aResponseSender);

                    // If this is the only sender then start the timer measuring the inactivity to detect if the client is disconnected.
                    // If it is not the only sender, then the timer is already running.
                    if (myResponseSenders.size() == 1)
                    {
                        myResponseReceiverInactivityTimer.schedule(getTimerTask(), myResponseReceiverInactivityTimeout);
                    }
                }

                return aResponseSender;
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
            // If polling.
            if (httpRequestContext.getHttpMethod().equals("GET"))
            {
                // Get responseReceiverId.
                String[] aQueryItems = { httpRequestContext.getUri().getQuery() };
                if (aQueryItems.length > 0)
                {
                    final String aResponseReceiverId = aQueryItems[0].substring(4);

                    // Find the sender for the response receiver.
                    HttpResponseSender aResponseSender = null;
                    synchronized (myResponseSenders)
                    {
                        aResponseSender = EnumerableExt.firstOrDefault(myResponseSenders, new IFunction1<Boolean, HttpResponseSender>()
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
            {
                byte[] aMessage = httpRequestContext.getRequestMessage();

                String aClientIp = httpRequestContext.getRemoteEndPoint();
                MessageContext aMessageContext = new MessageContext(aMessage, aClientIp, null);

                if (!myMessageHandler.invoke(aMessageContext))
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
            synchronized (myResponseSenders)
            {
                final long aTime = System.currentTimeMillis();

                // Check the connection for each connected duplex output channel.
                HashSetExt.removeWhere(myResponseSenders, new IFunction1<Boolean, HttpResponseSender>()
                {
                    @Override
                    public Boolean invoke(HttpResponseSender x) throws Exception
                    {
                        // If the last polling activity time exceeded the maximum allowed time then
                        // it is considered the connection is closed.
                        if (aTime - x.LastPollingActivityTime > myResponseReceiverInactivityTimeout)
                        {
                            // If the connection was broken unexpectidly then the message handler must be notified.
                            if (!x.IsDisposed)
                            {
                                MessageContext aMessageContext = new MessageContext(null, "", x);
                                myMessageHandler.invoke(aMessageContext);
                            }

                            // Indicate to remove the item.
                            return true;
                        }

                        // Indicate to keep the item.
                        return false;
                    }
                }); 

                // If there connected clients we need to check if they are active.
                if (myResponseSenders.size() > 0)
                {
                    myResponseReceiverInactivityTimer.schedule(getTimerTask(), myResponseReceiverInactivityTimeout);
                }
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
    
    
    private HttpListener myHttpListenerProvider;
    private IFunction1<Boolean, MessageContext> myMessageHandler;
    private HashSet<HttpResponseSender> myResponseSenders = new HashSet<HttpResponseSender>();
    private Timer myResponseReceiverInactivityTimer;
    private int myResponseReceiverInactivityTimeout;
    
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

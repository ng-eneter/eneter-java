/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.*;
import java.lang.Thread.State;
import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.*;
import eneter.net.system.threading.internal.ManualResetEvent;


class HttpOutputConnector implements IOutputConnector
{

    public HttpOutputConnector(String httpServiceConnectorAddress, String responseReceiverId, int pollingFrequencyMiliseconds)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // just check if the channel id is a valid Uri
                myUrl = new URL(httpServiceConnectorAddress);
            }
            catch (Exception err)
            {
                EneterTrace.error(httpServiceConnectorAddress + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            myResponseReceiverId = responseReceiverId;
            myPollingFrequencyMiliseconds = pollingFrequencyMiliseconds;
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
                myStopReceivingRequestedFlag = false;

                myResponseMessageHandler = responseMessageHandler;

                myStopPollingWaitingEvent.reset();
                myResponseReceiverThread = new Thread(myDoPolling);
                myResponseReceiverThread.start();

                // Wait until thread listening to response messages is running.
                if (!myListeningToResponsesStartedEvent.waitOne(1000))
                {
                    EneterTrace.warning(TracedObject() + "failed to start the thread listening to response messages within 1 second.");
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
            myStopReceivingRequestedFlag = true;
            myStopPollingWaitingEvent.set();
            
            if (myResponseReceiverThread != null && myResponseReceiverThread.getState() != State.NEW)
            {
                try
                {
                    myResponseReceiverThread.join(3000);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "detected an exception during waiting for ending of thread. The thread id = " + myResponseReceiverThread.getId());
                }
                
                if (myResponseReceiverThread.getState() != Thread.State.TERMINATED)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.StopThreadFailure + myResponseReceiverThread.getId());

                    try
                    {
                        myResponseReceiverThread.stop();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.AbortThreadFailure, err);
                    }
                }
            }
            myResponseReceiverThread = null;
            myResponseMessageHandler = null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        return myIsListeningToResponses;
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
            byte[] aMessage = (byte[])message;
            HttpRequestInvoker.invokePostRequest(myUrl, aMessage);
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
        throw new UnsupportedOperationException("toStreamWritter is not supported.");
    }
    
    private void doPolling()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myIsListeningToResponses = true;
            myListeningToResponsesStartedEvent.set();

            try
            {
                // Create URI for polling.
                String aParameters = "?id=" + myResponseReceiverId;
                URL aPollingUrl = new URL(myUrl.toString() + aParameters);

                while (!myStopReceivingRequestedFlag)
                {
                    // Send poll request to get messages from the service.
                    byte[] aResponseMessages = HttpRequestInvoker.invokeGetRequest(aPollingUrl);

                    if (!myStopReceivingRequestedFlag)
                    {
                        
                        if (aResponseMessages != null && aResponseMessages.length > 0)
                        {
                            // Convert the response to the fast memory stream
                            ByteArrayInputStream aBufferedMessages = new ByteArrayInputStream(aResponseMessages);
                            
                            MessageContext aMessageContext = new MessageContext(aBufferedMessages, myUrl.getHost(), null);
                            
                            while (!myStopReceivingRequestedFlag && aBufferedMessages.available() > 0)
                            {
                                if (!myResponseMessageHandler.invoke(aMessageContext))
                                {
                                    EneterTrace.warning(TracedObject() + "failed to process all response messages.");
                                    break;
                                }
                            }
                            
                        }

                        if (!myStopReceivingRequestedFlag)
                        {
                            myStopPollingWaitingEvent.waitOne(myPollingFrequencyMiliseconds);
                        }
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
            }

            myIsListeningToResponses = false;
            myListeningToResponsesStartedEvent.reset();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    private URL myUrl;
    private String myResponseReceiverId;
    private Thread myResponseReceiverThread;
    private volatile boolean myIsListeningToResponses;
    private volatile boolean myStopReceivingRequestedFlag;
    private ManualResetEvent myListeningToResponsesStartedEvent = new ManualResetEvent(false);
    private IFunction1<Boolean, MessageContext> myResponseMessageHandler;

    private int myPollingFrequencyMiliseconds;
    private ManualResetEvent myStopPollingWaitingEvent = new ManualResetEvent(false);
    
    private Runnable myDoPolling = new Runnable()
    {
        @Override
        public void run()
        {
            doPolling();
        }
    };
    
    private String TracedObject() { return getClass().getSimpleName() + " "; }
}

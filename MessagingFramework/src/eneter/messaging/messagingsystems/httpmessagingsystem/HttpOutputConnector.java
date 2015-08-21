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
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.*;
import eneter.net.system.threading.internal.ManualResetEvent;


class HttpOutputConnector implements IOutputConnector
{

    public HttpOutputConnector(String httpServiceConnectorAddress, String responseReceiverId, IProtocolFormatter protocolFormatter, int pollingFrequencyMiliseconds)
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
            myProtocolFormatter = protocolFormatter;
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
            
            try
            {
                myConnectionManipulatorLock.lock();
                try
                {
                    myStopReceivingRequestedFlag = false;
    
                    myResponseMessageHandler = responseMessageHandler;
    
                    myStopPollingWaitingEvent.reset();
                    myResponseReceiverThread = new Thread(myDoPolling, "Eneter.HttpPolling");
                    myResponseReceiverThread.start();
    
                    // Wait until thread listening to response messages is running.
                    if (!myListeningToResponsesStartedEvent.waitOne(1000))
                    {
                        EneterTrace.warning(TracedObject() + "failed to start the thread listening to response messages within 1 second.");
                    }
                    
                    // Send open connection message.
                    Object anEncodedMessage = myProtocolFormatter.encodeOpenConnectionMessage(myResponseReceiverId);
                    sendMessage(anEncodedMessage);
                }
                finally
                {
                    myConnectionManipulatorLock.unlock();
                }
            }
            catch (Exception err)
            {
                closeConnection();
                throw err;
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
            cleanConnection(true);
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
    public void sendRequestMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                Object anEncodedMessage = myProtocolFormatter.encodeMessage(myResponseReceiverId, message);
                sendMessage(anEncodedMessage);
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void sendMessage(Object message) throws Exception
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

                boolean aServiceClosedConnection = false;
                while (!myStopReceivingRequestedFlag && !aServiceClosedConnection)
                {
                    myStopPollingWaitingEvent.waitOne(myPollingFrequencyMiliseconds);
                    
                    if (!myStopReceivingRequestedFlag)
                    {
                        // Send poll request to get messages from the service.
                        byte[] aResponseMessages = HttpRequestInvoker.invokeGetRequest(aPollingUrl);
                        
                        if (aResponseMessages != null && aResponseMessages.length > 0)
                        {
                            // Convert the response to the fast memory stream
                            ByteArrayInputStream aBufferedResponse = new ByteArrayInputStream(aResponseMessages);
                            
                            while (!myStopReceivingRequestedFlag && !aServiceClosedConnection && aBufferedResponse.available() > 0)
                            {
                                IMethod1<MessageContext> aResponseHandler = myResponseMessageHandler;
                                
                                ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage((InputStream)aBufferedResponse);
                                MessageContext aMessageContext = new MessageContext(aProtocolMessage, myUrl.getHost());
                                
                                if (aProtocolMessage != null && aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
                                {
                                    aServiceClosedConnection = true;
                                }
                                
                                try
                                {
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
                        }
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.FailedInListeningLoop, err);
            }

            myIsListeningToResponses = false;
            myListeningToResponsesStartedEvent.reset();
            
            // If the connection was closed from the service.
            if (!myStopReceivingRequestedFlag)
            {
                IMethod1<MessageContext> aResponseHandler = myResponseMessageHandler;
                cleanConnection(false);

                try
                {
                    aResponseHandler.invoke(null);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void cleanConnection(boolean sendMessageFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                myStopReceivingRequestedFlag = true;
                myStopPollingWaitingEvent.set();

                if (sendMessageFlag)
                {
                    // Send close connection message.
                    try
                    {
                        Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(myResponseReceiverId);
                        sendMessage(anEncodedMessage);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to send close connection message.", err);
                    }
                }

                if (myResponseReceiverThread != null && Thread.currentThread().getId() != myResponseReceiverThread.getId())
                {
                    if (myResponseReceiverThread.getState() != State.NEW)
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
                            EneterTrace.warning(TracedObject() + ErrorHandler.FailedToStopThreadId + myResponseReceiverThread.getId());
        
                            try
                            {
                                myResponseReceiverThread.stop();
                            }
                            catch (Exception err)
                            {
                                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToAbortThread, err);
                            }
                        }
                    }
                }
                myResponseReceiverThread = null;
                myResponseMessageHandler = null;
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private URL myUrl;
    private String myResponseReceiverId;
    private IProtocolFormatter myProtocolFormatter;
    private Thread myResponseReceiverThread;
    private volatile boolean myIsListeningToResponses;
    private volatile boolean myStopReceivingRequestedFlag;
    private ManualResetEvent myListeningToResponsesStartedEvent = new ManualResetEvent(false);
    private IMethod1<MessageContext> myResponseMessageHandler;

    private int myPollingFrequencyMiliseconds;
    private ManualResetEvent myStopPollingWaitingEvent = new ManualResetEvent(false);
    private ThreadLock myConnectionManipulatorLock = new ThreadLock();
    
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

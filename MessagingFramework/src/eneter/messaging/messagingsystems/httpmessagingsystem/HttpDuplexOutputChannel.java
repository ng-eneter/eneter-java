package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.*;
import java.lang.Thread.State;
import java.net.*;
import java.util.UUID;

import eneter.messaging.dataprocessing.messagequeueing.WorkingThread;
import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.*;

class HttpDuplexOutputChannel implements IDuplexOutputChannel
{
    public HttpDuplexOutputChannel(String channelId, String responseReceiverId, int pullingFrequencyMiliseconds, IProtocolFormatter<byte[]> protocolFormatter)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(channelId))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }

            try
            {
                myUrl = new URL(channelId);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }


            myChannelId = channelId;
            myPollingFrequencyMiliseconds = pullingFrequencyMiliseconds;

            // Creates the working thread with the message queue for processing incoming response messages.
            myResponseMessageWorkingThread = new WorkingThread<ProtocolMessage>(getChannelId());

            myResponseReceiverId = (StringExt.isNullOrEmpty(responseReceiverId)) ? channelId + "_" + UUID.randomUUID().toString() : responseReceiverId;

            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public Event<DuplexChannelMessageEventArgs> responseMessageReceived()
    {
        return myResponseMessageReceivedEventApi;
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpenedEventApi;
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosedEventApi;
    }

    @Override
    public String getChannelId()
    {
        return myChannelId;
    }

    @Override
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }

    @Override
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

                try
                {
                    myStopHttpResponseListeningRequested = false;

                    // Register the method handling messages from the working with the queue.
                    // Note: Received responses are put to the message queue. This thread takes
                    //       messages from the queue and calls the registered method to process them.
                    myResponseMessageWorkingThread.registerMessageHandler(myMessageHandlerHandler);

                    // Create thread responsible for the loop listening to response messages coming from
                    // the Http duplex input channel.
                    myStopPollingWaitingEvent.reset();
                    myResponseListener = new Thread(myPollingRunnable);
                    myResponseListener.start();

                    // Encode the open connection message.
                    byte[] anEncodedMessage = myProtocolFormatter.encodeOpenConnectionMessage(getResponseReceiverId());

                    // Send the request to open the connection.
                    HttpClient.sendOnewayRequest(myUrl, anEncodedMessage);

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

    @Override
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                // Indicate that the processing of response messages should stop.
                // Note: Thread listening to response messages checks this flag and stops the looping.
                myStopHttpResponseListeningRequested = true;
                myStopPollingWaitingEvent.set();

                // Try to notify the server that the connection is closed.
                try
                {
                    if (!StringExt.isNullOrEmpty(getResponseReceiverId()))
                    {
                        // Encode the message to close the connection.
                        byte[] anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(getResponseReceiverId());

                        // Send the request to close the connection.
                        HttpClient.sendOnewayRequest(myUrl, anEncodedMessage);
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                }
                catch (Error err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                    throw err;
                }

                // Wait until the polling stops.
                if (myResponseListener != null && myResponseListener.getState() != State.NEW)
                {
                    try
                    {
                        myResponseListener.join(5000);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "detected an exception during waiting for ending of thread. The thread id = " + myResponseListener.getId());
                    }
                    
                    if (myResponseListener.getState() != Thread.State.TERMINATED)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.StopThreadFailure + myResponseListener.getId());

                        try
                        {
                            myResponseListener.stop();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.AbortThreadFailure, err);
                        }
                        catch (Error err)
                        {
                            EneterTrace.error(TracedObject() + ErrorHandler.AbortThreadFailure, err);
                            throw err;
                        }
                    }
                }
                myResponseListener = null;

                // Stop the thread processing polled message.
                try
                {
                    myResponseMessageWorkingThread.unregisterMessageHandler();
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

    @Override
    public boolean isConnected()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                return myIsListeningToResponses;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    @Override
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
                    byte[] anEncodedMessage = myProtocolFormatter.encodeMessage(getResponseReceiverId(), message);
                    
                    // Send the message.
                    HttpClient.sendOnewayRequest(myUrl, anEncodedMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                    throw err;
                }
                catch (Error err)
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
    
    private void doPolling()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myIsListeningToResponses = true;

            try
            {
                while (!myStopHttpResponseListeningRequested)
                {
                    // Encode the request for polling messages.
                    byte[] anEncodedMessage = myProtocolFormatter.encodePollRequest(getResponseReceiverId());
                   
                    byte[] aResponseMessages = HttpClient.sendRequest(myUrl, anEncodedMessage);
                    if (aResponseMessages != null && aResponseMessages.length > 0)
                    {
                        ByteArrayInputStream aBufferedMessages = new ByteArrayInputStream(aResponseMessages);
                        
                        // Decode message by message.
                        // Note: available() returns count - pos  in case of ByteArrayInputStream.
                        while (aBufferedMessages.available() > 0)
                        {
                            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(aBufferedMessages);
                            
                            if (aProtocolMessage != null && aProtocolMessage.MessageType != EProtocolMessageType.Unknown)
                            {
                                // Put the message to the message queue from where it will be processed
                                // by the working thread.
                                myResponseMessageWorkingThread.enqueueMessage(aProtocolMessage);
                            }
                            else
                            {
                                EneterTrace.warning(TracedObject() + "failed to decode response messages.");
                                break;
                            }
                        }
                    }

                    // Wait specified time before next polling.
                    myStopPollingWaitingEvent.waitOne(myPollingFrequencyMiliseconds);
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
                throw err;
            }

            // Stop the thread processing polled message.
            try
            {
                myResponseMessageWorkingThread.unregisterMessageHandler();
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.UnregisterMessageHandlerThreadFailure, err);
            }

            myIsListeningToResponses = false;

            // Notify the listening to messages stoped.
            notifyConnectionClosed();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void handleResponseMessage(ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (protocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                // Close connection with the duplex input channel.
                // Note: The Close() must be called from the different thread because
                //       it will try to stop this thread (thread processing messages).
                Runnable aConnectionClosing = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        closeConnection();
                    }
                };
                ThreadPool.queueUserWorkItem(aConnectionClosing);
            }
            else if (protocolMessage.MessageType != EProtocolMessageType.MessageReceived)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
            }
            else if (myResponseMessageReceivedEventImpl.isEmpty() == false)
            {
                try
                {
                    DuplexChannelMessageEventArgs anEvent = new DuplexChannelMessageEventArgs(getChannelId(), protocolMessage.Message, getResponseReceiverId());
                    myResponseMessageReceivedEventImpl.update(this, anEvent);
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
                            if (myConnectionOpenedEventImpl.isEmpty() == false)
                            {
                                DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId());
                                myConnectionOpenedEventImpl.update(this, aMsg);
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
    
    private void notifyConnectionClosed()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Execute the callback in a different thread.
            // The problem is, the event handler can call back to the duplex output channel - e.g. trying to open
            // connection - and since this closing is not finished and this thread would be blocked, .... => problems.
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
                            if (myConnectionClosedEventImpl.isEmpty() == false)
                            {
                                DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId());
                                myConnectionClosedEventImpl.update(this, aMsg);
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
    
    
    private String myChannelId;
    private String myResponseReceiverId;
    
    private IProtocolFormatter<byte[]> myProtocolFormatter;

    private Object myConnectionManipulatorLock = new Object();

    private Thread myResponseListener;
    private volatile boolean myStopHttpResponseListeningRequested;
    private volatile boolean myIsListeningToResponses;
    
    private WorkingThread<ProtocolMessage> myResponseMessageWorkingThread;
    
    private URL myUrl;
    
    private int myPollingFrequencyMiliseconds;
    private ManualResetEvent myStopPollingWaitingEvent = new ManualResetEvent(false);
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private Event<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventApi = new Event<DuplexChannelMessageEventArgs>(myResponseMessageReceivedEventImpl);
    
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private Event<DuplexChannelEventArgs> myConnectionOpenedEventApi = new Event<DuplexChannelEventArgs>(myConnectionOpenedEventImpl);
    
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private Event<DuplexChannelEventArgs> myConnectionClosedEventApi = new Event<DuplexChannelEventArgs>(myConnectionClosedEventImpl);
    
    
    private Runnable myPollingRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    doPolling();
                }
            };
    
    private IMethod1<ProtocolMessage> myMessageHandlerHandler = new IMethod1<ProtocolMessage>()
            {
                @Override
                public void invoke(ProtocolMessage message) throws Exception
                {
                    handleResponseMessage(message);
                }
            };
    
    
    private String TracedObject()
    {
        return "Http duplex output channel '" + getChannelId() + "' "; 
    }
}

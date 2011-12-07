package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.UUID;

import eneter.messaging.dataprocessing.messagequeueing.WorkingThread;
import eneter.messaging.dataprocessing.streaming.MessageStreamer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.ManualResetEvent;
import eneter.net.system.threading.ThreadPool;

class TcpDuplexOutputChannel implements IDuplexOutputChannel
{
    public TcpDuplexOutputChannel(String ipAddressAndPort, String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(ipAddressAndPort))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }

            try
            {
                // just check if the address is valid
                new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            myChannelId = ipAddressAndPort;

            myResponseReceiverId = (StringExt.isNullOrEmpty(responseReceiverId)) ? ipAddressAndPort + "_" + UUID.randomUUID().toString() : responseReceiverId;
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
                    Object[] aMessage = MessageStreamer.getRequestMessage(getResponseReceiverId(), message); 

                    // Store the message in the buffer
                    byte[] aBufferedMessage = null;
                    ByteArrayOutputStream aMemStream = new ByteArrayOutputStream();
                    try
                    {
                        MessageStreamer.writeMessage(aMemStream, aMessage);
                        aBufferedMessage = aMemStream.toByteArray();
                    }
                    finally
                    {
                        aMemStream.close();
                    }

                    OutputStream aSendStream = myTcpClient.getOutputStream();
                    
                    // Send the message from the buffer.
                    aSendStream.write(aBufferedMessage);
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


                // If it is needed clear after previous connection
                if (myTcpClient != null)
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
                    myStopReceivingRequestedFlag = false;

                    URI aUri = new URI(getChannelId());
                    myTcpClient = new Socket(InetAddress.getByName(aUri.getHost()), aUri.getPort());
                    myTcpClient.setTcpNoDelay(true);
                    
                    myMessageProcessingThread.registerMessageHandler(myMessageHandlerHandler);

                    myResponseReceiverThread = new Thread(myResponseListeningRunnable);
                    myResponseReceiverThread.start();
                    
                    // Wait until the thread is really started.
                    if (!myListeningToResponsesStartedEvent.waitOne(500))
                    {
                        // The listening thread did not start.
                        throw new IllegalStateException("The thread listening to response messages did not start.");
                    }

                    // Send open connection message with receiver id.
                    MessageStreamer.writeOpenConnectionMessage(myTcpClient.getOutputStream(), getResponseReceiverId());

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

    @SuppressWarnings("deprecation")
    @Override
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                myStopReceivingRequestedFlag = true;

                if (myTcpClient != null)
                {
                    // Try to notify that the connection is closed
                    if (!StringExt.isNullOrEmpty(getResponseReceiverId()))
                    {
                        try
                        {
                            Object[] aMessage = MessageStreamer.getCloseConnectionMessage(getResponseReceiverId());
                            MessageStreamer.writeMessage(myTcpClient.getOutputStream(), aMessage);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                        }
                    }

                    try
                    {
                        // This will close the connection with the server and it should
                        // also release the thread waiting for a response message.
                        myTcpClient.close();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to stop Tcp connection.", err);
                    }

                    myTcpClient = null;
                }

                if (myResponseReceiverThread != null && myResponseReceiverThread.getState() != Thread.State.NEW)
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
                        catch (Error err)
                        {
                            EneterTrace.error(TracedObject() + ErrorHandler.AbortThreadFailure, err);
                            throw err;
                        }
                    }
                }
                myResponseReceiverThread = null;

                try
                {
                    myMessageProcessingThread.unregisterMessageHandler();
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
                return myTcpClient != null && myIsListeningToResponses;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private void doResponseListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Indicate, the listening thread is running.
            myIsListeningToResponses = true;
            myListeningToResponsesStartedEvent.set();

            try
            {
                while (!myStopReceivingRequestedFlag)
                {
                    Object aMessage = MessageStreamer.readMessage(myTcpClient.getInputStream());

                    if (!myStopReceivingRequestedFlag && aMessage != null)
                    {
                        myMessageProcessingThread.enqueueMessage(aMessage);
                    }

                    // If disconnected
                    if (aMessage == null || myTcpClient == null || !myTcpClient.isConnected())
                    {
                        EneterTrace.warning(TracedObject() + "detected the duplex input channel is not available. The connection will be closed.");
                        break;
                    }
                }
            }
            catch (SocketException err)
            {
                // If the server is not listening, then this exception occurred because the listening
                // was stopped and tracing is not desired.
                // If the server still listens, then there is some other problem that must be traced.
                if (myTcpClient != null)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
            }

            // Stop the thread processing messages
            try
            {
                myMessageProcessingThread.unregisterMessageHandler();
            }
            catch (Exception err)
            {
                // We need just to close it, therefore we are not interested about exception.
                // They can be ignored here.
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
    
    private void messageHandler(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseMessageReceivedEventImpl.isEmpty() == false)
            {
                try
                {
                    myResponseMessageReceivedEventImpl.update(this, new DuplexChannelMessageEventArgs(getChannelId(), message, getResponseReceiverId()));
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
    
    private Socket myTcpClient;
    private Object myConnectionManipulatorLock = new Object();

    private Thread myResponseReceiverThread;
    private volatile boolean myStopReceivingRequestedFlag;
    private volatile boolean myIsListeningToResponses;
    private ManualResetEvent myListeningToResponsesStartedEvent = new ManualResetEvent(false);
    
    private WorkingThread<Object> myMessageProcessingThread = new WorkingThread<Object>();
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private Event<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventApi = new Event<DuplexChannelMessageEventArgs>(myResponseMessageReceivedEventImpl);
    
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private Event<DuplexChannelEventArgs> myConnectionOpenedEventApi = new Event<DuplexChannelEventArgs>(myConnectionOpenedEventImpl);
    
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private Event<DuplexChannelEventArgs> myConnectionClosedEventApi = new Event<DuplexChannelEventArgs>(myConnectionClosedEventImpl);
    
    
    private Runnable myResponseListeningRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            doResponseListening();
        }
    };
    
    private IMethod1<Object> myMessageHandlerHandler = new IMethod1<Object>()
            {
                @Override
                public void invoke(Object message) throws Exception
                {
                    messageHandler(message);
                }
            };
    
    private String TracedObject()
    {
        String aChannelId = (getChannelId() != null) ? getChannelId() : "";
        return "The Tcp duplex output channel '" + aChannelId + "' ";
    }
}

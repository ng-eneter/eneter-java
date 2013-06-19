/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.OutputStream;
import java.net.*;
import java.util.UUID;

import eneter.messaging.dataprocessing.messagequeueing.WorkingThread;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.IpAddressUtil;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;
import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ThreadPool;

class TcpDuplexOutputChannel implements IDuplexOutputChannel
{
    public TcpDuplexOutputChannel(String ipAddressAndPort, String responseReceiverId,
                                  IProtocolFormatter<byte[]> protocolFormatter,
                                  IClientSecurityFactory clientSecurotyFactory) throws Exception
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
            
            mySocketAddress = new InetSocketAddress(aUri.getHost(), aUri.getPort());

            myChannelId = ipAddressAndPort;

            myResponseReceiverId = (StringExt.isNullOrEmpty(responseReceiverId)) ? ipAddressAndPort + "_" + UUID.randomUUID().toString() : responseReceiverId;
            
            myProtocolFormatter = protocolFormatter;
            
            myClientSecurityFactory = clientSecurotyFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public Event<DuplexChannelMessageEventArgs> responseMessageReceived()
    {
        return myResponseMessageReceivedEventImpl.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpenedEventImpl.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosedEventImpl.getApi();
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

                    myTcpClient = myClientSecurityFactory.createClientSocket(mySocketAddress);
                    
                    myIpAddress = IpAddressUtil.getLocalIpAddress(myTcpClient);
                    
                    myMessageProcessingThread.registerMessageHandler(myMessageHandlerHandler);

                    myResponseReceiverThread = new Thread(myResponseListeningRunnable);
                    myResponseReceiverThread.start();
                    
                    // Wait until the thread is really started.
                    if (!myListeningToResponsesStartedEvent.waitOne(500))
                    {
                        // The listening thread did not start.
                        throw new IllegalStateException("The thread listening to response messages did not start.");
                    }

                    // Encode the request to open the connection.
                    byte[] anEncodedMessage = myProtocolFormatter.encodeOpenConnectionMessage(getResponseReceiverId());
                    
                    // Send open connection message with receiver id.
                    myTcpClient.getOutputStream().write(anEncodedMessage);

                    // Invoke the event notifying, the connection was opened.
                    notifyEvent(myConnectionOpenedEventImpl);
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
                            // Encode the message to close the connection.
                            byte[] anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(getResponseReceiverId());

                            // Send the message.
                            myTcpClient.getOutputStream().write(anEncodedMessage);
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
                        EneterTrace.warning(TracedObject() + "failed to close the socket.", err);
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
                    byte[] anEncodedMessage = myProtocolFormatter.encodeMessage(getResponseReceiverId(), message); 

                    OutputStream aSendStream = myTcpClient.getOutputStream();
                    
                    // Send the message from the buffer.
                    aSendStream.write(anEncodedMessage);
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
                    // Decode the incoming message.
                    ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(myTcpClient.getInputStream());

                    if (!myStopReceivingRequestedFlag && aProtocolMessage != null)
                    {
                        myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                    }

                    // If disconnected
                    if (aProtocolMessage == null || myTcpClient == null || !myTcpClient.isConnected())
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
            myListeningToResponsesStartedEvent.reset();

            // Notify the listening to messages stopped.
            notifyEvent(myConnectionClosedEventImpl);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void messageHandler(ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (protocolMessage.MessageType != EProtocolMessageType.MessageReceived)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                return;
            }
            
            if (myResponseMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myResponseMessageReceivedEventImpl.raise(this, new DuplexChannelMessageEventArgs(getChannelId(), protocolMessage.Message, getResponseReceiverId(), myIpAddress));
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
    
    private void notifyEvent(final EventImpl<DuplexChannelEventArgs> eventHandler)
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
                            if (eventHandler.isSubscribed())
                            {
                                DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId(), myIpAddress);
                                eventHandler.raise(this, aMsg);
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
            });
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    
    private String myChannelId;
    private String myResponseReceiverId;
    
    private IClientSecurityFactory myClientSecurityFactory;
    private Socket myTcpClient;
    private String myIpAddress;
    private InetSocketAddress mySocketAddress;
    private Object myConnectionManipulatorLock = new Object();

    private Thread myResponseReceiverThread;
    private volatile boolean myStopReceivingRequestedFlag;
    private volatile boolean myIsListeningToResponses;
    private ManualResetEvent myListeningToResponsesStartedEvent = new ManualResetEvent(false);
    
    private WorkingThread<ProtocolMessage> myMessageProcessingThread = new WorkingThread<ProtocolMessage>();
    
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    
    
    private Runnable myResponseListeningRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            doResponseListening();
        }
    };
    
    private IMethod1<ProtocolMessage> myMessageHandlerHandler = new IMethod1<ProtocolMessage>()
            {
                @Override
                public void invoke(ProtocolMessage message) throws Exception
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

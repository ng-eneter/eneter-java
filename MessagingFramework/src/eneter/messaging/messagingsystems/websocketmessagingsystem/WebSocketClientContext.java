/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.Map;

import eneter.messaging.dataprocessing.messagequeueing.MessageQueue;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.net.system.*;
import eneter.net.system.internal.Cast;
import eneter.net.system.threading.internal.ThreadPool;

class WebSocketClientContext implements IWebSocketClientContext
{
    private enum EMessageInSendProgress
    {
        None,
        Binary,
        Text
    }
    
    
    public WebSocketClientContext(URI address, Map<String, String> headerFields, Socket tcpClient)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAddress = address;
            myHeaderFields = headerFields;
            myTcpClient = tcpClient;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public Event<Object> connectionClosed()
    {
        return myConnectionClosedEvent.getApi();
    }
    
    public Event<Object> pongReceived()
    {
        return myPongReceivedEvent.getApi();
    }
    
    
    
    
    public URI getUri()
    {
        return myAddress;
    }
    
    public Map<String, String> getHeaderFields()
    {
        return Collections.unmodifiableMap(myHeaderFields);
    }
    
    public InetSocketAddress getClientEndPoint()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return Cast.as(myTcpClient.getRemoteSocketAddress(), InetSocketAddress.class);
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
                return myTcpClient != null && myIsListeningToResponses;
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
                myStopReceivingRequestedFlag = true;
                myMessageInSendProgress = EMessageInSendProgress.None;

                if (myTcpClient != null)
                {
                    // Try to send the frame closing the communication.
                    try
                    {
                        // Generate the masking key.
                        byte[] aCloseFrame = WebSocketFormatter.encodeCloseFrame(null, (short)1000);
                        myTcpClient.getOutputStream().write(aCloseFrame);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                    }

                    try
                    {
                        myTcpClient.close();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to close Tcp connection.", err);
                    }

                    myTcpClient = null;
                }

                myReceivedMessages.unblockProcessingThreads();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void sendMessage(Object data) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            sendMessage(data, true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void sendMessage(final Object data, final boolean isFinal) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                // If there is no message that was not finalized yet then send the binary or text data frame.
                if (myMessageInSendProgress == EMessageInSendProgress.None)
                {
                    if (data instanceof byte[])
                    {
                        sendFrame(new IFunction1<byte[], byte[]>()
                        {
                            @Override
                            public byte[] invoke(byte[] maskingKey) throws Exception
                            {
                                return WebSocketFormatter.encodeBinaryMessageFrame(isFinal, maskingKey, (byte[])data);
                            }
                        });

                        if (isFinal == false)
                        {
                            myMessageInSendProgress = EMessageInSendProgress.Binary;
                        }
                    }
                    else if (data instanceof String)
                    {
                        sendFrame(new IFunction1<byte[], byte[]>()
                        {
                            @Override
                            public byte[] invoke(byte[] maskingKey) throws Exception
                            {
                                return WebSocketFormatter.encodeTextMessageFrame(isFinal, maskingKey, (String)data);
                            }
                        });
                        
                        if (isFinal == false)
                        {
                            myMessageInSendProgress = EMessageInSendProgress.Text;
                        }
                    }
                    else
                    {
                        String anErrorMessage = TracedObject() + "failed to send the message because input parameter data is not byte[] or string.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalArgumentException(anErrorMessage);
                    }
                }
                // If there is a binary message that was sent only partialy - was not finalized yet.
                else if (myMessageInSendProgress == EMessageInSendProgress.Binary)
                {
                    if (data instanceof byte[])
                    {
                        sendFrame(new IFunction1<byte[], byte[]>()
                        {
                            @Override
                            public byte[] invoke(byte[] maskingKey) throws Exception
                            {
                                return WebSocketFormatter.encodeContinuationMessageFrame(isFinal, maskingKey, (byte[])data);
                            }
                        });

                        if (isFinal == true)
                        {
                            myMessageInSendProgress = EMessageInSendProgress.None;
                        }
                    }
                    else
                    {
                        String anErrorMessage = TracedObject() + "failed to send the continuation binary message because input parameter data was not byte[].";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalArgumentException(anErrorMessage);
                    }
                }
                // If there is a text message that was sent only partialy - was not finalized yet.
                else
                {
                    if (data instanceof String)
                    {
                        sendFrame(new IFunction1<byte[], byte[]>()
                        {
                            @Override
                            public byte[] invoke(byte[] maskingKey) throws Exception
                            {
                                return WebSocketFormatter.encodeContinuationMessageFrame(isFinal, maskingKey, (String)data);
                            }
                        });

                        if (isFinal == true)
                        {
                            myMessageInSendProgress = EMessageInSendProgress.None;
                        }
                    }
                    else
                    {
                        String anErrorMessage = TracedObject() + "failed to send the continuation text message because input parameter data was not string.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalArgumentException(anErrorMessage);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public void sendPing() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            sendFrame(new IFunction1<byte[], byte[]>()
            {
                @Override
                public byte[] invoke(byte[] maskingKey) throws Exception
                {
                    return WebSocketFormatter.encodePingFrame(maskingKey);
                }
            });
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void sendPong() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            sendFrame(new IFunction1<byte[], byte[]>()
            {
                @Override
                public byte[] invoke(byte[] maskingKey) throws Exception
                {
                    return WebSocketFormatter.encodePongFrame(maskingKey, null);
                }
            });
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public WebSocketMessage receiveMessage() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myReceivedMessages.dequeueMessage();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void sendFrame(IFunction1<byte[], byte[]> formatter) throws Exception
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
                    // Encode the message frame.
                    // Note: According to the protocol, server shall not mask sent data.
                    byte[] aFrame = formatter.invoke(null);

                    // Send the message.
                    myTcpClient.getOutputStream().write(aFrame);
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
    
    public void doRequestListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myIsListeningToResponses = true;
            short aCloseCode = 0;

            try
            {
                PipedOutputStream aCurrentMessageStream = null;

                while (!myStopReceivingRequestedFlag)
                {
                    // Decode the incoming message.
                    final WebSocketFrame aFrame = WebSocketFormatter.decodeFrame(myTcpClient.getInputStream());

                    if (!myStopReceivingRequestedFlag && aFrame != null)
                    {
                        // Frames from server must be unmasked.
                        // According the protocol, If the frame was NOT masked, the server must close connection with the client.
                        if (aFrame.MaskFlag == false)
                        {
                            throw new IllegalStateException(TracedObject() + "received unmasked frame from the client. Frames from client shall be masked.");
                        }

                        // Process the frame.
                        if (aFrame.FrameType == EFrameType.Ping)
                        {
                            // Response 'pong'. The response responses same data as received in the 'ping'.
                            sendFrame(new IFunction1<byte[], byte[]>()
                            {
                                @Override
                                public byte[] invoke(byte[] maskingKey) throws Exception
                                {
                                    return WebSocketFormatter.encodePongFrame(maskingKey, aFrame.Message);
                                }
                            });
                        }
                        else if (aFrame.FrameType == EFrameType.Close)
                        {
                            EneterTrace.debug(TracedObject() + "received the close frame.");
                            break;
                        }
                        else if (aFrame.FrameType == EFrameType.Pong)
                        {
                            notify(myPongReceivedEvent);
                        }
                        // If a new binary message starts.
                        else if (aFrame.FrameType == EFrameType.Binary || aFrame.FrameType == EFrameType.Text)
                        {
                            // If a previous message is not finished then the new message is not expected -> protocol error.
                            if (aCurrentMessageStream != null)
                            {
                                EneterTrace.warning(TracedObject() + "detected unexpected new message. (previous message was not finished)");

                                // Protocol error close code.
                                aCloseCode = 1002;
                                break;
                            }

                            // Create stream where the message data will be writen.
                            aCurrentMessageStream = new PipedOutputStream();
                            PipedInputStream anMessageInputStream = new PipedInputStream(aCurrentMessageStream);
                            aCurrentMessageStream.write(aFrame.Message);

                            // If this frame is also the final frame then set the stream to unblocking mode.
                            // Note: Due to performance, unblock the stream before the event is sent.
                            //       So that the client will not wait for unblock.
                            if (aFrame.IsFinal)
                            {
                                aCurrentMessageStream.close();
                            }

                            // Put received message to the queue.
                            WebSocketMessage aReceivedMessage = new WebSocketMessage(aFrame.FrameType == EFrameType.Text, anMessageInputStream);
                            myReceivedMessages.enqueueMessage(aReceivedMessage);

                            if (aFrame.IsFinal)
                            {
                                // The message is finalized.
                                aCurrentMessageStream = null;
                            }
                        }
                        // If a message continues. (I.e. message is split into more fragments.)
                        else if (aFrame.FrameType == EFrameType.Continuation)
                        {
                            // If the message does not exist then continuing frame does not have any sense -> protocol error.
                            if (aCurrentMessageStream == null)
                            {
                                EneterTrace.warning(TracedObject() + "detected unexpected continuing of a message. (none message was started before)");

                                // Protocol error close code.
                                aCloseCode = 1002;
                                break;
                            }

                            aCurrentMessageStream.write(aFrame.Message);

                            // If this is the final frame.
                            if (aFrame.IsFinal)
                            {
                                aCurrentMessageStream.close();
                                aCurrentMessageStream = null;
                            }
                        }
                    }

                    // If disconnected
                    if (aFrame == null || !myTcpClient.isConnected())
                    {
                        //EneterTrace.Warning(TracedObject + "detected the TCP connection is not available. The connection will be closed.");
                        break;
                    }
                }
            }
            catch (IOException err)
            {
                // Ignore this exception. It is often thrown when the connection was closed.
                // Do not thrace this because the tracing degradates the performance in this case.
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
            }

            // If the connection is being closed due to a protocol error.
            if (aCloseCode > 1000)
            {
                // Try to send the close message.
                try
                {
                    byte[] aCloseMessage = WebSocketFormatter.encodeCloseFrame(null, aCloseCode);
                    myTcpClient.getOutputStream().write(aCloseMessage);
                }
                catch (Exception err)
                {
                }
            }

            myIsListeningToResponses = false;

            myReceivedMessages.unblockProcessingThreads();

            // Notify the listening to messages stoped.
            notify(myConnectionClosedEvent);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notify(final EventImpl<Object> eventHandler)
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
                            if (eventHandler.isSubscribed())
                            {
                                eventHandler.raise(this, new Object());
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
    
    
    private URI myAddress;
    private Socket myTcpClient;
    
    private Map<String, String> myHeaderFields;

    private Object myConnectionManipulatorLock = new Object();

    private boolean myStopReceivingRequestedFlag;
    private boolean myIsListeningToResponses;
    
    private EMessageInSendProgress myMessageInSendProgress = EMessageInSendProgress.None;
    private MessageQueue<WebSocketMessage> myReceivedMessages = new MessageQueue<WebSocketMessage>();
    
    
    private EventImpl<Object> myConnectionClosedEvent = new EventImpl<Object>();
    private EventImpl<Object> myPongReceivedEvent = new EventImpl<Object>();
    
    private String TracedObject()
    {
        return "WebSocketClientContext " + getUri() + " ";
    }
}

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
import java.util.*;

import eneter.messaging.dataprocessing.messagequeueing.WorkingThread;
import eneter.messaging.dataprocessing.streaming.DynamicInputStream;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.OutputStreamTimeoutWriter;
import eneter.net.system.*;
import eneter.net.system.internal.Cast;
import eneter.net.system.internal.Convert;
import eneter.net.system.internal.StringExt;
import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ThreadPool;

/**
 * WebSocket client.
 * Represents the client for the websocket communication.
 * <br/>
 * The following example shows how to communicate with a websocket server.
 * <pre>
 * {@code
 * class MyClient
 * {
 * 
 *     private EventHandler<WebSocketMessage> myOnResponseMessageReceived = EventHandler<WebSocketMessage>()
 *         {
 *             public void onEvent(Object sender, WebSocketMessage e)
 *             {
 *                 onResponseMessageReceived(sender, e);
 *             }
 *         }
 * 
 *     private WebSocketClient myClient;
 * 
 * 
 *     public MyClass() throws Exception
 *     {
 *         // Instantiate websocket client.
 *         myClient = new WebSocketClient("ws://127.0.0.1:8045/MyService/");
 *         
 *         // Subscribe to receive messages.
 *         myClient.messageReceived().subscribe(myOnResponseMessageReceived);
 *     }
 * 
 *     public void openConnection()
 *     {
 *         // Open the connection.
 *         myClient.openConnection();
 *     }
 *     
 *     public void closeConnection()
 *     {
 *         // Close the connection.
 *         myClient.closeConnection();
 *     }
 * 
 *     void sendHello()
 *     {
 *         // Send a text message.
 *         myClient.sendMessage("Hello.");
 *     }
 *     
 *     // Method called when a response message from the server is received.
 *     private void onResponseMessageReceived(Object sender, WebSocketMessage e)
 *     {
 *         try
 *         {
 *             if (y.isText())
 *             {
 *                 // If server responded the text message.
 *                 String aTextMessage = e.getWholeTextMessage();
 *                 
 *                 ...
 *             }
 *             else
 *             {
 *                 // The server responded the binary message.
 *                 byte[] aBinaryMessage = e.getWholeMessage();
 *                 
 *                 ...
 *             }
 *         }
 *         catch (Exception err)
 *         {
 *            ...
 *         }
 *     }
 * }
 * }
 * </pre>
 *
 */
public class WebSocketClient
{
    private enum EMessageInSendProgress
    {
        None,
        Binary,
        Text
    }
    
    // Identifies who is responsible for starting the thread listening to response messages.
    // The point is, if nobody is subscribed to receive response messages, then we can significantly improve
    // the performance if the listening thread is not started.
    private enum EResponseListeningResponsible
    {
        // Open connection method is responsible for starting threads that will loop and receive incoming messages.
        OpenConnection,

        // Subscribing to MessageReceived or ConnectionClosed or PongReceived will start threads looping for incoming messages.
        EventSubscription,

        // Looping threads are already running, so nobody is supposed to start it.
        Nobody
    }
    
    private class CustomEvent<T> implements Event<T>
    {
        public CustomEvent(Event<T> originalEvent)
        {
            myOriginalEvent = originalEvent;
        }
        
        @Override
        public void subscribe(EventHandler<T> eventHandler)
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (myResponsibleForActivatingListening == EResponseListeningResponsible.EventSubscription)
                {
                    activateResponseListening();
                }
                myOriginalEvent.subscribe(eventHandler);
            }
        }

        @Override
        public void unsubscribe(EventHandler<T> eventHandler)
        {
            myOriginalEvent.unsubscribe(eventHandler);
        }
        
        private Event<T> myOriginalEvent;
    }
    
    /**
     * Constructs the websocket client.
     * 
     * @param address websocket uri address. Provide port number too. e.g. ws://127.0.0.1:8055/myservice/<br/>
     * You can also specify the query that can be used to pass some open connection related parameters.
     * e.g. ws://127.0.0.1:8055/myservice/?param1=10&param2=20
     */
    public WebSocketClient(URI address)
    {
        this(address, new NoneSecurityClientFactory());
    }
    
    /**
     * Constructs the websocket client.
     * 
     * @param address websocket uri address. Provide port number too. e.g. ws://127.0.0.1:8055/myservice/<br/>
     * You can also specify the query that can be used to pass some open connection related parameters.
     * e.g. ws://127.0.0.1:8055/myservice/?param1=10&param2=20
     * @param clientSecurityFactory Factory allowing SSL communication.
     */
    public WebSocketClient(URI address, IClientSecurityFactory clientSecurityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAddress = address;
            myClientSecurityFactory = clientSecurityFactory;
            mySocketAddress = new InetSocketAddress(address.getHost(), address.getPort());
            
            myHeaderFields.put("Host", myAddress.getAuthority());
            myHeaderFields.put("Upgrade", "websocket");
            myHeaderFields.put("Connection", "Upgrade");
            myHeaderFields.put("Sec-WebSocket-Version", "13");
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Event is invoked when the connection is open.
     * @return
     */
    public Event<Object> connectionOpened()
    {
        return myConnectionOpenedEvent.getApi();
    }
    
    /**
     * Event is invoked when the connection is closed.
     * @return
     */
    public Event<Object> connectionClosed()
    {
        CustomEvent<Object> aCustomEvent = new CustomEvent<Object>(myConnectionClosedEvent.getApi());
        return aCustomEvent;
    }
    
    /**
     * Event is invoked when the pong is received. E.g. when the server responded ping.
     * @return
     */
    public Event<Object> pongReceived()
    {
        CustomEvent<Object> aCustomEvent = new CustomEvent<Object>(myPongReceivedEvent.getApi());
        return aCustomEvent;
    }
    
    /**
     * The event is invoked when a data message from server is received.
     * @return
     */
    public Event<WebSocketMessage> messageReceived()
    {
        CustomEvent<WebSocketMessage> aCustomEvent = new CustomEvent<WebSocketMessage>(myMessageReceivedEvent.getApi());
        return aCustomEvent;
    }
    
    public void setConnectionTimeout(int connectionTimeout)
    {
        myClientSecurityFactory.setConnectionTimeout(connectionTimeout);
    }
    
    public int getConnectionTimeout()
    {
        return myClientSecurityFactory.getConnectionTimeout();
    }
    
    public void setSendTimeout(int sendTimeout)
    {
        myClientSecurityFactory.setSendTimeout(sendTimeout);
    }
    
    public int getSendTimeout()
    {
        return myClientSecurityFactory.getSendTimeout();
    }
    
    public void setReceiveTimeout(int receiveTimeout)
    {
        myClientSecurityFactory.setReceiveTimeout(receiveTimeout);
    }
    
    public int getReceiveTimeout()
    {
        return myClientSecurityFactory.getReceiveTimeout();
    }
    
    /**
     * Returns address of websocket server.
     * @return
     */
    public URI getUri()
    {
        return myAddress;
    }
    
    /**
     * Allows to get and set header-fields which shall be sent in open connection request.
     * @return
     */
    public HashMap<String, String> getHeaderFields()
    {
        return myHeaderFields;
    }
    
    public InetSocketAddress getLocalEndPoint()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (myTcpClient != null)
                {
                    return Cast.as(myTcpClient.getLocalSocketAddress(), InetSocketAddress.class);
                }
                
                return null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Returns true if the connection to the server is open.
     * @return
     */
    public boolean isConnected()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                return myTcpClient != null && isResponseSubscribed() == false ||
                       myTcpClient != null && isResponseSubscribed() == true && myIsListeningToResponses == true;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Opens connection to the websocket server.
     * @throws Exception
     */
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
                        closeTcp();
                    }
                    catch (Exception err)
                    {
                        // We tried to clean after the previous connection. The exception can be ignored.
                    }
                }

                try
                {
                    myStopReceivingRequestedFlag = false;

                    // Generate the key for this connection.
                    byte[] aWebsocketKey = new byte[16];
                    myGenerator.nextBytes(aWebsocketKey);
                    String aKey64baseEncoded = Convert.toBase64String(aWebsocketKey);
                    myHeaderFields.put("Sec-WebSocket-Key", aKey64baseEncoded);

                    // Send HTTP request to open the websocket communication.
                    String aPathAndQuery = myAddress.getPath();
                    if (!StringExt.isNullOrEmpty(myAddress.getQuery()))
                    {
                        aPathAndQuery += "?" + myAddress.getQuery();
                    }
                    byte[] anOpenRequest = WebSocketFormatter.encodeOpenConnectionHttpRequest(aPathAndQuery, myHeaderFields);

                    // Open TCP connection.
                    myTcpClient = myClientSecurityFactory.createClientSocket(mySocketAddress);
                    myStreamWriter.write(myTcpClient.getOutputStream(), anOpenRequest, myClientSecurityFactory.getSendTimeout());
                    
                    // Wait for the HTTP response and check if the connection was open.
                    HashMap<String, String> aResponseResult = WebSocketFormatter.decodeOpenConnectionHttpResponse(myTcpClient.getInputStream());
                    
                    validateOpenConnectionResponse(aResponseResult, aWebsocketKey);

                    // If somebody is subscribed to receive some response messages then
                    // the bidirectional communication is needed and listening threads must be activated.
                    if (isResponseSubscribed())
                    {
                        activateResponseListening();
                    }
                    else
                    {
                        // Nobody is subscribed so delegate the responsibility to start listening threads
                        // to the point when somebody subscribes to receive some response messages like
                        // CloseConnection, Pong, MessageReceived.
                        myResponsibleForActivatingListening = EResponseListeningResponsible.EventSubscription;
                    }

                    // Wait until the listening thread is running.
                    //myListeningToResponsesStartedEvent.WaitOne(1000);

                    // Notify opening the websocket connection.
                    // Note: the notification is executed from a different thread.
                    notify(myConnectionOpenedEvent);
                }
                catch (Exception err)
                {
                    try
                    {
                        closeTcp();
                    }
                    catch (Exception err2)
                    {
                    }

                    EneterTrace.error(TracedObject() + ErrorHandler.OpenConnectionFailure, err);
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Closes connection with the webscocket server.
     * It sends the close message to the service and closes the underlying tcp connection.
     */
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
                    try
                    {
                        // Generate the masking key.
                        byte[] aMaskingKey = getMaskingKey();
                        byte[] aCloseFrame = WebSocketFormatter.encodeCloseFrame(aMaskingKey, (short)1000);
                        myStreamWriter.write(myTcpClient.getOutputStream(), aCloseFrame, myClientSecurityFactory.getSendTimeout());
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

                if (myResponseReceiverThread != null && myResponseReceiverThread.getState() != Thread.State.NEW)
                {
                    try
                    {
                        myResponseReceiverThread.join(3000);
                    }
                    catch(Exception err)
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

                // Reset the responsibility for starting of threads looping for response messages.
                myResponsibleForActivatingListening = EResponseListeningResponsible.OpenConnection;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Sends message to the client.
     * The message must be type of string or byte[]. If the type is string then the message is sent as the text message via text frame.
     * If the type is byte[] the message is sent as the binary message via binary frame.
     * 
     * @param data message to be sent to the client. Must be byte[] or string.
     * @throws Exception If sending of the message failed.
     */
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
    
    /**
     * Sends message to the client. Allows to send the message via multiple frames.
     * The message must be type of string or byte[]. If the type is string then the message is sent as the text message via text frame.
     * If the type is byte[] the message is sent as the binary message via binary frame.<br/>
     * It allows to send the message in multiple frames. The client then can receive all parts separately
     * using WebSocketMessage.InputStream or as a whole message using WebSocketMessage.GetWholeMessage().
     * <br/>
     * The following example shows how to send 'Hello world.' in three parts.
     * <pre>
     * {@code
     *     
     *     // Send the first part of the message.
     *     aClient.sendMessage("Hello ", false);
     *     
     *     // Send the second part.
     *     aClient.sendMessage("wo", false);
     *     
     *     // Send the third final part.
     *     aClient.sendMessage("rld.", true);
     * }
     * </pre>
     * 
     * @param data message to be sent to the client. The message can be byte[] or string.
     * @param isFinal true if this is the last part of the message.
     * @throws Exception If sending of the message failed.
     */
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
    
    /**
     * Pings the server. According to websocket protocol, pong should be responded.
     * @throws Exception
     */
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
    
    /**
     * Sends unsolicited pong to the server.
     * @throws Exception
     */
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
    
    
    private void validateOpenConnectionResponse(HashMap<String, String> responseRegExResult, byte[] webSocketKey)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aSecurityAccept = responseRegExResult.get("Sec-WebSocket-Accept");

            // If some required header field is missing or has incorrect value.
            if (!responseRegExResult.containsKey("Upgrade") ||
                !responseRegExResult.containsKey("Connection") ||
                StringExt.isNullOrEmpty(aSecurityAccept))
            {
                String anErrorMessage = TracedObject() + ErrorHandler.OpenConnectionFailure + " A required header field was missing.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            // Check the value of websocket accept.
            String aWebSocketKeyBase64 = Convert.toBase64String(webSocketKey);
            String aCalculatedAcceptance = WebSocketFormatter.encryptWebSocketKey(aWebSocketKeyBase64);
            if (!aCalculatedAcceptance.equals(aSecurityAccept))
            {
                String anErrorMessage = TracedObject() + ErrorHandler.OpenConnectionFailure + " Sec-WebSocket-Accept has incorrect value.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
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
                    byte[] aMaskingKey = getMaskingKey();
                    byte[] aFrame = formatter.invoke(aMaskingKey);

                    myStreamWriter.write(myTcpClient.getOutputStream(), aFrame, myClientSecurityFactory.getSendTimeout());
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
    
    private void activateResponseListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (!myIsListeningToResponses)
            {
                // Start thread processing decoded messages from the queue.
                myMessageProcessingThread.registerMessageHandler(new IMethod1<WebSocketMessage>()
                {
                    @Override
                    public void invoke(WebSocketMessage webSocketMessage) throws Exception
                    {
                        messageHandler(webSocketMessage);
                    }
                });

                // Start listening to frames responded by websocket server.
                //ThreadPool.QueueUserWorkItem(x => DoResponseListening());
                myResponseReceiverThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        doResponseListening();
                    }
                });
                myResponseReceiverThread.start();

                // Wait until the listening thread is running.
                try
                {
                    myListeningToResponsesStartedEvent.waitOne(1000);
                }
                catch (Exception err)
                {
                }

                // Listening to response messages is active. So nobody is responsible to activate it.
                myResponsibleForActivatingListening = EResponseListeningResponsible.Nobody;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    // Note: aContinuousMessageStream cannot be close() because it is put to the WebSocketMessage
    @SuppressWarnings("resource")
    private void doResponseListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myIsListeningToResponses = true;
            myListeningToResponsesStartedEvent.set();
            short aCloseCode = 0;

            try
            {
                
                DynamicInputStream aContinuousMessageStream = null;

                while (!myStopReceivingRequestedFlag)
                {
                    // Decode the incoming message.
                    final WebSocketFrame aFrame = WebSocketFormatter.decodeFrame(myTcpClient.getInputStream());

                    if (!myStopReceivingRequestedFlag && aFrame != null)
                    {
                        // Frames from server must be unmasked.
                        // According the protocol, If the frame was masked, the client must close connection with the server.
                        if (aFrame.MaskFlag)
                        {
                            throw new IllegalStateException(TracedObject() + "received masked frame from the server. Frames from server shall be unmasked.");
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
                        // If a new message starts.
                        else if (aFrame.FrameType == EFrameType.Binary || aFrame.FrameType == EFrameType.Text)
                        {
                            // If a previous message is not finished then the new message is not expected -> protocol error.
                            if (aContinuousMessageStream != null)
                            {
                                EneterTrace.warning(TracedObject() + "detected unexpected new message. (previous message was not finished)");

                                // Protocol error close code.
                                aCloseCode = 1002;
                                break;
                            }
                            
                            WebSocketMessage aReceivedMessage = null;

                            // If the message does not come in multiple frames then optimize the performance
                            // and use MemoryStream instead of DynamicStream.
                            if (aFrame.IsFinal)
                            {
                                ByteArrayInputStream aMessageStream = new ByteArrayInputStream(aFrame.Message);
                                aReceivedMessage = new WebSocketMessage(aFrame.FrameType == EFrameType.Text, aMessageStream);
                            }
                            // if the message is split to several frames then use DynamicStream so that writing of incoming
                            // frames and reading of already received data can run in parallel.
                            else
                            {
                                // Create stream where the message data will be written.
                                aContinuousMessageStream = new DynamicInputStream();
                                aContinuousMessageStream.writeWithoutCopying(aFrame.Message, 0, aFrame.Message.length);
                                aReceivedMessage = new WebSocketMessage(aFrame.FrameType == EFrameType.Text, aContinuousMessageStream);
                            }

                            // Put received message to the queue from where the processing thread will invoke the event MessageReceived.
                            // Note: user will get events always in the same thread.
                            myMessageProcessingThread.enqueueMessage(aReceivedMessage);
                        }
                        // If a message continues. (I.e. message is split into more fragments.)
                        else if (aFrame.FrameType == EFrameType.Continuation)
                        {
                            // If the message does not exist then continuing frame does not have any sense -> protocol error.
                            if (aContinuousMessageStream == null)
                            {
                                EneterTrace.warning(TracedObject() + "detected unexpected continuing of a message. (none message was started before)");

                                // Protocol error close code.
                                aCloseCode = 1002;
                                break;
                            }

                            aContinuousMessageStream.writeWithoutCopying(aFrame.Message, 0, aFrame.Message.length);

                            // If this is the final frame.
                            if (aFrame.IsFinal)
                            {
                                aContinuousMessageStream.setBlockingMode(false);
                                
                                // Note: aContinuousMessageStream cannot be close() because it is put to the WebSocketMessage
                                aContinuousMessageStream = null;
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
                    byte[] aMaskingKey = getMaskingKey();
                    byte[] aCloseMessage = WebSocketFormatter.encodeCloseFrame(aMaskingKey, aCloseCode);

                    myStreamWriter.write(myTcpClient.getOutputStream(), aCloseMessage, myClientSecurityFactory.getSendTimeout());
                }
                catch (Exception err)
                {
                }
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

            // Notify the listening to messages stoped.
            notify(myConnectionClosedEvent);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
        private void closeTcp()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                try
                {
                    myMessageInSendProgress = EMessageInSendProgress.None;
    
    
                    if (myTcpClient != null)
                    {
                        myTcpClient.close();
                        myTcpClient = null;
                    }
                }
                catch (Exception err)
                {
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
    
    private byte[] getMaskingKey()
    {
        byte[] aKey = new byte[4];
        myGenerator.nextBytes(aKey);
        return aKey;
    }
    
    private void messageHandler(WebSocketMessage message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageReceivedEvent.isSubscribed())
            {
                try
                {
                    myMessageReceivedEvent.raise(this, message);
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
    
    
    private boolean isResponseSubscribed()
    {
        // If somebody is subscribed for response messages, then we need bidirectional communication.
        // Note: It means, the thread listening to responses and the thread responsible for processing messages from the queue
        //       are supposed to be started.
        return myMessageReceivedEvent.isSubscribed() || myConnectionClosedEvent.isSubscribed() || myPongReceivedEvent.isSubscribed();
    }

    
    
    private URI myAddress;
    private InetSocketAddress mySocketAddress;
    private IClientSecurityFactory myClientSecurityFactory;
    private Socket myTcpClient;
    
    private HashMap<String, String> myHeaderFields = new HashMap<String, String>();
    
    private Random myGenerator = new Random();
    
    private Object myConnectionManipulatorLock = new Object();
    private EResponseListeningResponsible myResponsibleForActivatingListening = EResponseListeningResponsible.OpenConnection;
    
    private Thread myResponseReceiverThread;
    private boolean myStopReceivingRequestedFlag;
    private boolean myIsListeningToResponses;
    private ManualResetEvent myListeningToResponsesStartedEvent = new ManualResetEvent(false);
    
    private EMessageInSendProgress myMessageInSendProgress = EMessageInSendProgress.None;
    
    private WorkingThread<WebSocketMessage> myMessageProcessingThread = new WorkingThread<WebSocketMessage>();
    
    private OutputStreamTimeoutWriter myStreamWriter = new OutputStreamTimeoutWriter();
    
    private EventImpl<Object> myConnectionOpenedEvent = new EventImpl<Object>();
    private EventImpl<Object> myConnectionClosedEvent = new EventImpl<Object>();
    private EventImpl<Object> myPongReceivedEvent = new EventImpl<Object>();
    private EventImpl<WebSocketMessage> myMessageReceivedEvent = new EventImpl<WebSocketMessage>();
    
    
    private String TracedObject()
    {
        return "WebSocketClient " + getUri() + " ";
    }
}

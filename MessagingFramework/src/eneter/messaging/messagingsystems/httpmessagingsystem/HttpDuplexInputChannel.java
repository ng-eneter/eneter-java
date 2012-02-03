/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.*;
import java.net.Socket;
import java.util.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpInputChannelBase;
import eneter.net.system.*;
import eneter.net.system.collections.generic.HashSetExt;
import eneter.net.system.linq.EnumerableExt;


class HttpDuplexInputChannel extends TcpInputChannelBase
                             implements IDuplexInputChannel
{
    private static class TResponseReceiver
    {
        public enum EConnctionState
        {
            Open,
            Close
        }

        public TResponseReceiver(String responseReceiverId, long creationTime)
        {
            myResponseReceiverId = responseReceiverId;
            myLastPollingActivityTime = System.currentTimeMillis();
            myConnectionState = EConnctionState.Open;
            myMessages = new ArrayDeque<byte[]>();
        }

        public String getResponseReceiverId()
        {
            return myResponseReceiverId;
        }
        
        public EConnctionState getConnectionState()
        {
            return myConnectionState;
        }
        
        public void setConnectionState(EConnctionState state)
        {
            myConnectionState = state;
        }
        
        public long getLastPollingActivityTime()
        {
            return myLastPollingActivityTime;
        }
        
        public void setLastPollingActivityTime(long time)
        {
            myLastPollingActivityTime = time;
        }
        
        public Queue<byte[]> getMessages()
        {
            return myMessages;
        }
        
        private String myResponseReceiverId;
        private EConnctionState myConnectionState;
        private long myLastPollingActivityTime;
        private ArrayDeque<byte[]> myMessages;
    }
    
    public HttpDuplexInputChannel(String channelId, int responseReceiverInactivityTimeout,
            IProtocolFormatter<byte[]> protocolFormatter,
            IServerSecurityFactory serverSecurityFactory)
            throws Exception
    {
        super(channelId,
              new HttpListenerProvider(channelId, serverSecurityFactory),
              serverSecurityFactory);
        
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Initialize the timer to regularly check the timeout for connections with duplex output channels.
            // If the duplex output channel did not pull within the timeout then the connection
            // is closed and removed from the list.
            // Note: The timer is set here but not executed.
            myResponseReceiverInactivityTimer = new Timer("HttpResponseReceiverInactivityTimer", true);
    
            myResponseReceiverInactivityTimeout = responseReceiverInactivityTimeout;
    
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public Event<DuplexChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventImpl.getApi();
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
    public void sendResponseMessage(final String responseReceiverId, Object message)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myResponseMessages)
            {
                TResponseReceiver aResponsesForParticularReceiver = EnumerableExt.firstOrDefault(myResponseMessages,
                        new IFunction1<Boolean, TResponseReceiver>()
                        {
                            @Override
                            public Boolean invoke(TResponseReceiver x)
                                    throws Exception
                            {
                                return x.getResponseReceiverId().equals(responseReceiverId);
                            }
                        });
                        
                if (aResponsesForParticularReceiver != null && aResponsesForParticularReceiver.getConnectionState() == TResponseReceiver.EConnctionState.Open)
                {
                    // Encode the response message.
                    byte[] anEncodedMessage = myProtocolFormatter.encodeMessage(responseReceiverId, message);

                    // Enqueue the response message.
                    // Note: When the client polls then the messages are picked up from the queue.
                    aResponsesForParticularReceiver.getMessages().add(anEncodedMessage);
                }
                else
                {
                    String aMessage = TracedObject() + ErrorHandler.SendResponseNotConnectedFailure;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void disconnectResponseReceiver(final String responseReceiverId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myResponseMessages)
            {
                TResponseReceiver aResponsesForParticularReceiver = EnumerableExt.firstOrDefault(myResponseMessages,
                        new IFunction1<Boolean, TResponseReceiver>()
                        {
                            @Override
                            public Boolean invoke(TResponseReceiver x)
                                    throws Exception
                            {
                                return x.getResponseReceiverId().equals(responseReceiverId);
                            }
                        });
                        
                if (aResponsesForParticularReceiver != null)
                {
                    // Set the status, that the connection is closed.
                    aResponsesForParticularReceiver.setConnectionState(TResponseReceiver.EConnctionState.Close);

                    // Encode the message notifying the client that was disconnected.
                    byte[] anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(responseReceiverId);

                    // Encqueue the message.
                    // Note: When the client polls, then the message is picked up from the queue.
                    aResponsesForParticularReceiver.getMessages().add(anEncodedMessage);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void disconnectClients() throws IOException
    {
        // Not applicable for HTTP.
    }

    @Override
    protected void handleConnection(Socket clientSocket) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Status that will be responded back to the client.
            // If everything is OK, the status will be changed to OK.
            ByteArrayOutputStream aResponseMessages = null; // will be set if response messages are available
            int aSizeOfResponseMessages = 0;
            int anHttpStatusCode = 404; // will be set to 200 if everything is OK.
            
            InputStream anInputStream = null;

            try
            {
                // Source stream.
                // Note: The stream position is set right behind the HTTP part.
                anInputStream = clientSocket.getInputStream();
                
                // Decode the eneter message.
                final ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(anInputStream);

                if (aProtocolMessage != null)
                {
                    // If it is a normal message.
                    if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                    {
                        synchronized (myResponseMessages)
                        {
                            // If the sending duplex output channel is connected then process the message.
                            // Otherwise return with an error.
                            TResponseReceiver aResponseReceiver = EnumerableExt.firstOrDefault(myResponseMessages,
                                    new IFunction1<Boolean, TResponseReceiver>()
                                    {
                                        @Override
                                        public Boolean invoke(TResponseReceiver x)
                                                throws Exception
                                        {
                                            return x.getResponseReceiverId().equals(aProtocolMessage.ResponseReceiverId);
                                        }
                                    });

                            if (aResponseReceiver != null && aResponseReceiver.getConnectionState() == TResponseReceiver.EConnctionState.Open)
                            {
                                myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                                
                                // Everything is OK.
                                anHttpStatusCode = 200;
                            }
                        }
                    }
                    // If it is a request to poll enqueued messages.
                    else if (aProtocolMessage.MessageType == EProtocolMessageType.PollRequest)
                    {
                        try
                        {
                            synchronized (myResponseMessages)
                            {
                                // Get messages collected for the response receiver.
                                TResponseReceiver aResponsesForParticularReceiver = EnumerableExt.firstOrDefault(myResponseMessages,
                                        new IFunction1<Boolean, TResponseReceiver>()
                                        {
                                            @Override
                                            public Boolean invoke(TResponseReceiver x)
                                                    throws Exception
                                            {
                                                return x.getResponseReceiverId().equals(aProtocolMessage.ResponseReceiverId);
                                            }
                                        });
                                
                                if (aResponsesForParticularReceiver != null)
                                {
                                    // Update the polling time.
                                    aResponsesForParticularReceiver.setLastPollingActivityTime(System.currentTimeMillis());

                                    // If there are stored messages for the receiver
                                    if (aResponsesForParticularReceiver.getMessages().size() > 0)
                                    {
                                        aResponseMessages = new ByteArrayOutputStream();
                                        
                                        // Dequeue responses to be sent to the response receiver.
                                        // Note: Try not to exceed 1MB - better do more small transfers
                                        while (aResponsesForParticularReceiver.getMessages().size() > 0 &&
                                                aSizeOfResponseMessages < 1048576)
                                        {
                                            // Get the response message formatted according to the connection protocol.
                                            byte[] aResponseMessage = aResponsesForParticularReceiver.getMessages().poll();
                                            aResponseMessages.write(aResponseMessage, 0, aResponseMessage.length);
                                            
                                            aSizeOfResponseMessages += aResponseMessage.length;
                                        }
                                    }
                                }
                                
                                // Everything ok.
                                anHttpStatusCode = 200;
                            }
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
                        }
                        catch (Error err)
                        {
                            EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
                            throw err;
                        }
                    }
                    else
                    {
                        // Put the message to the queue from where the working thread removes it to notify
                        // subscribers of the input channel.
                        // Note: therfore subscribers of the input channel are notified allways in one thread.
                        myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                        
                        // Everything ok.
                        anHttpStatusCode = 200;
                    }
                }
                else
                {
                    EneterTrace.warning(TracedObject() + "did not receive a valid message.");
                }
            }
            finally
            {
                // Send HTTP response.
                OutputStream anOutputStream = clientSocket.getOutputStream();
                DataOutputStream aWriter = new DataOutputStream(anOutputStream);
                if (anHttpStatusCode == 200)
                {
                    if (aResponseMessages != null && aSizeOfResponseMessages > 0)
                    {
                        aWriter.writeBytes("HTTP/1.1 200 OK\r\n");
                        aWriter.writeBytes("Content-Type: \r\n");
                        aWriter.writeBytes("Content-Length: " + aSizeOfResponseMessages + "\r\n\r\n");
                        
                        // Send also eneter response message in the context of HTTP.
                        aResponseMessages.writeTo(anOutputStream);
                    }
                    else
                    {
                        aWriter.writeBytes("HTTP/1.1 200 OK\r\n\r\n");
                    }
                }
                else
                {
                    aWriter.writeBytes("HTTP/1.1 404 Not Found\r\n\r\n");
                }
                
                clientSocket.close();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void handleMessage(final ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // Open connection request.
                if (protocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
                {
                    openConnectionIfNeeded(protocolMessage.ResponseReceiverId);
                    notifyResponseReceiverConnected(protocolMessage.ResponseReceiverId);
                }
                // Close connection request.
                else if (protocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
                {
                    int aNumberOfRemovedResponseReceivers;

                    synchronized (myResponseMessages)
                    {
                        // Delete the response receiver and its context.
                        aNumberOfRemovedResponseReceivers = HashSetExt.removeWhere(myResponseMessages,
                                new IFunction1<Boolean, TResponseReceiver>()
                                {
                                    @Override
                                    public Boolean invoke(TResponseReceiver x)
                                            throws Exception
                                    {
                                        return x.getResponseReceiverId().equals(protocolMessage.ResponseReceiverId);
                                    }
                                });
                    }

                    if (aNumberOfRemovedResponseReceivers > 0)
                    {
                        notifyResponseReceiverDisconnected(protocolMessage.ResponseReceiverId);
                    }
                }
                // Notify the incoming message request.
                else if (protocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                {
                    notifyMessageReceived(getChannelId(), protocolMessage.Message, protocolMessage.ResponseReceiverId);
                }
                else
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ReceiveMessageFailure, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private void openConnectionIfNeeded(final String responseReceiverId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myResponseMessages)
            {
                TResponseReceiver aResponseReceiver = EnumerableExt.firstOrDefault(myResponseMessages,
                        new IFunction1<Boolean, TResponseReceiver>()
                        {
                            @Override
                            public Boolean invoke(TResponseReceiver x) throws Exception
                            {
                                return x.getResponseReceiverId().equals(responseReceiverId);
                            }
                        });

                // If the response receiver is not registered yet.
                // E.g. because 'open connection' message came somehow after this message.
                // "Open" connection for the response receiver. -> store response receiver id.
                if (aResponseReceiver == null)
                {
                    aResponseReceiver = new TResponseReceiver(responseReceiverId, System.currentTimeMillis());
                    myResponseMessages.add(aResponseReceiver);
                    
                    // If the timer is not running, then start it.
                    if (myResponseMessages.size() == 1)
                    {
                        myResponseReceiverInactivityTimer.schedule(getTimerTask(), myResponseReceiverInactivityTimeout);
                    }
                }

                aResponseReceiver.setConnectionState(TResponseReceiver.EConnctionState.Open);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyResponseReceiverConnected(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverConnectedEventImpl.isSubscribed())
            {
                ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId);
    
                try
                {
                    myResponseReceiverConnectedEventImpl.raise(this, aResponseReceiverEvent);
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
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyResponseReceiverDisconnected(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverDisconnectedEventImpl.isSubscribed())
            {
                ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId);

                try
                {
                    myResponseReceiverDisconnectedEventImpl.raise(this, aResponseReceiverEvent);
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
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyMessageReceived(String channelId, Object message, String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myMessageReceivedEventImpl.raise(this, new DuplexChannelMessageEventArgs(channelId, message, responseReceiverId));
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
    
    
    private void onConnectionCheckTimer()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myResponseMessages)
            {
                // Get current time.
                long aCurrentTime = System.currentTimeMillis();

                // Check the connection for each connected duplex output channel.
                for (TResponseReceiver aResponseReceiver : myResponseMessages)
                {
                    // If the last polling activity time exceeded the maximum allowed time, then close connection.
                    if (aCurrentTime - aResponseReceiver.getLastPollingActivityTime() > myResponseReceiverInactivityTimeout)
                    {
                        // Mark the connection as closed.
                        aResponseReceiver.setConnectionState(TResponseReceiver.EConnctionState.Close);

                        // Create message to notify the client, the connection was cosed.
                        ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, aResponseReceiver.getResponseReceiverId(), null);

                        // Enqueue the message.
                        myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                    }
                }

                if (myResponseMessages.size() > 0)
                {
                    myResponseReceiverInactivityTimer.schedule(getTimerTask(), myResponseReceiverInactivityTimeout);
                }
            }
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
                onConnectionCheckTimer();
            }
        };
        
        return aTimerTask;
    }
    
    
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    
    private HashSet<TResponseReceiver> myResponseMessages = new HashSet<TResponseReceiver>();
    
    private long myResponseReceiverInactivityTimeout;
    private Timer myResponseReceiverInactivityTimer;
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    
    
    @Override
    protected String TracedObject()
    {
        return "Http duplex input channel '" + getChannelId() + "' ";
    }

}

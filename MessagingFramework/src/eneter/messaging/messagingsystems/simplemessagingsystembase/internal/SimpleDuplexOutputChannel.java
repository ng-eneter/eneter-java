/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.security.InvalidParameterException;
import java.util.UUID;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;
import eneter.net.system.threading.internal.ThreadPool;

public class SimpleDuplexOutputChannel implements IDuplexOutputChannel
{
    public SimpleDuplexOutputChannel(String channelId, String responseReceiverId, IMessagingSystemFactory messagingFactory,
                                     IProtocolFormatter<?> protocolFormatter) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(channelId))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new InvalidParameterException(ErrorHandler.NullOrEmptyChannelId);
            }
    
            myChannelId = channelId;
            myMessagingFactory = messagingFactory;
    
            myResponseReceiverId = (StringExt.isNullOrEmpty(responseReceiverId)) ? channelId + "_" + UUID.randomUUID().toString() : responseReceiverId;
    
            // Try to create input channel to check, if the response receiver id is correct.
            // If not, the exception will be thrown and the error is detected early.
            myMessagingFactory.createInputChannel(myResponseReceiverId);
    
            myMessageSenderOutputChannel = myMessagingFactory.createOutputChannel(channelId);
            
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
    public void openConnection()
        throws Exception
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
                    // Create the input channel listening to responses.
                    myResponseReceiverInputChannel = myMessagingFactory.createInputChannel(myResponseReceiverId);
                    myResponseReceiverInputChannel.messageReceived().subscribe(myResponseMessageReceivedHandler);
                    myResponseReceiverInputChannel.startListening();
    
                    // Send open connection message with receiver id.
                    Object anEncodedMessage = myProtocolFormatter.encodeOpenConnectionMessage(myResponseReceiverId); 
                    myMessageSenderOutputChannel.sendMessage(anEncodedMessage);
                    
                    // Invoke the event notifying, the connection was opened.
                    notifyConnectionOpened();
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.OpenConnectionFailure, err);
                    closeConnection();
    
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
                // Try to notify that the connection is closed
                if (myMessageSenderOutputChannel != null && !StringExt.isNullOrEmpty(myResponseReceiverId))
                {
                    try
                    {
                        Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(myResponseReceiverId);
                        myMessageSenderOutputChannel.sendMessage(anEncodedMessage);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                    }
                }
    
                stopListening();
            }
    
            notifyConnectionClosed();
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
                return (myResponseReceiverInputChannel != null);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void sendMessage(Object message)
        throws Exception
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
                    Object anEncodedMessage = myProtocolFormatter.encodeMessage(myResponseReceiverId, message); 
                    myMessageSenderOutputChannel.sendMessage(anEncodedMessage);
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

    private void onResponseMessageReceived(Object sender, ChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Decode the incoming message.
            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(e.getMessage());
    
            // If the message indicates the disconnection.
            if (aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                // Stop listening to the input channel for the response message.
                // Note: The duplex input channel notifies that this duplex output channel is disconnected.
                //       Therefore, it is not needed, this channel will send the message closing the connection.
                //       Therefore, it is enough just to stop listening to response messages.
                stopListening();
    
                notifyConnectionClosed();
            }
            // It is the normal response message - notify the subscribed handler.
            else if (myResponseMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myResponseMessageReceivedEventImpl.raise(this, new DuplexChannelMessageEventArgs(getChannelId(), aProtocolMessage.Message, myResponseReceiverId));
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
    
    private void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (myResponseReceiverInputChannel != null)
                {
                    // Stop the listener
                    try
                    {
                        myResponseReceiverInputChannel.stopListening();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.StopListeningFailure, err);
                    }
                    finally
                    {
                        myResponseReceiverInputChannel.messageReceived().unsubscribe(myResponseMessageReceivedHandler);
                        myResponseReceiverInputChannel = null;
                    }
                }
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
            Runnable aConnectionOpenedInvoker = new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if (myConnectionOpenedEventImpl.isSubscribed())
                        {
                            DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), myResponseReceiverId);
                            myConnectionOpenedEventImpl.raise(this, aMsg);
                        }
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    }
                }
            };
            
            ThreadPool.queueUserWorkItem(aConnectionOpenedInvoker);
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
            Runnable aConnectionClosedInvoker = new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if (myConnectionClosedEventImpl.isSubscribed())
                        {
                            DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), myResponseReceiverId);
                            myConnectionClosedEventImpl.raise(this, aMsg);
                        }
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    }
                }
            };
    
            // Invoke the event in a different thread.
            ThreadPool.queueUserWorkItem(aConnectionClosedInvoker);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IMessagingSystemFactory myMessagingFactory;

    private IInputChannel myResponseReceiverInputChannel;
    private IOutputChannel myMessageSenderOutputChannel;
    
    private String myChannelId;
    private String myResponseReceiverId;

    private Object myConnectionManipulatorLock = new Object();
    
    private IProtocolFormatter<?> myProtocolFormatter;
    

    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    
    
    private EventHandler<ChannelMessageEventArgs> myResponseMessageReceivedHandler = new EventHandler<ChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object t1, ChannelMessageEventArgs t2)
        {
            onResponseMessageReceived(t1, t2);
        }
    };
    

    private String TracedObject()
    {
        return "The duplex output channel '" + getChannelId() + "' ";
    }
}
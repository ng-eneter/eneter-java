package eneter.messaging.messagingsystems.simplemessagingsystembase;

import java.security.InvalidParameterException;
import java.util.UUID;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.ThreadPool;

public class SimpleDuplexOutputChannel implements IDuplexOutputChannel
{
    public SimpleDuplexOutputChannel(String channelId, String responseReceiverId, IMessagingSystemFactory messagingFactory,
                                     IProtocolFormatter<?> protocolFormatter) throws Exception
    {
        if (channelId == null || channelId.isEmpty())
        {
            EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
            throw new InvalidParameterException(ErrorHandler.NullOrEmptyChannelId);
        }

        myChannelId = channelId;
        myMessagingFactory = messagingFactory;

        myResponseReceiverId = (responseReceiverId == null || responseReceiverId.isEmpty()) ? channelId + "_" + UUID.randomUUID().toString() : responseReceiverId;

        // Try to create input channel to check, if the response receiver id is correct.
        // If not, the exception will be thrown and the error is detected early.
        myMessagingFactory.createInputChannel(myResponseReceiverId);

        myMessageSenderOutputChannel = myMessagingFactory.createOutputChannel(channelId);
        
        myProtocolFormatter = protocolFormatter;
    }
    
    @Override
    public Event<DuplexChannelMessageEventArgs> responseMessageReceived()
    {
        return myResponseMessageReceivedEvent;
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpenedEvent;
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosedEvent;
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

    @Override
    public void closeConnection()
    {
        synchronized (myConnectionManipulatorLock)
        {
            // Try to notify that the connection is closed
            if (myMessageSenderOutputChannel != null && myResponseReceiverId != null && !myResponseReceiverId.isEmpty())
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

    @Override
    public boolean isConnected()
    {
        synchronized (myConnectionManipulatorLock)
        {
            return (myResponseReceiverInputChannel != null);
        }
    }
    
    @Override
    public void sendMessage(Object message)
        throws Exception
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

    private void onResponseMessageReceived(Object sender, ChannelMessageEventArgs e)
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
        else if (!myResponseMessageReceivedEventImpl.isEmpty())
        {
            try
            {
                myResponseMessageReceivedEventImpl.update(this, new DuplexChannelMessageEventArgs(getChannelId(), aProtocolMessage.Message, myResponseReceiverId));
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
    
    private void stopListening()
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
    
    private void notifyConnectionOpened()
    {
        Runnable aConnectionOpenedInvoker = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (!myConnectionOpenedEventImpl.isEmpty())
                    {
                        DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), myResponseReceiverId);
                        myConnectionOpenedEventImpl.update(this, aMsg);
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
    
    private void notifyConnectionClosed()
    {
        Runnable aConnectionClosedInvoker = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (!myConnectionClosedEventImpl.isEmpty())
                    {
                        DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), myResponseReceiverId);
                        myConnectionClosedEventImpl.update(this, aMsg);
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
    
    
    private IMessagingSystemFactory myMessagingFactory;

    private IInputChannel myResponseReceiverInputChannel;
    private IOutputChannel myMessageSenderOutputChannel;
    
    private String myChannelId;
    private String myResponseReceiverId;

    private Object myConnectionManipulatorLock = new Object();
    
    private IProtocolFormatter<?> myProtocolFormatter;
    

    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private Event<DuplexChannelMessageEventArgs> myResponseMessageReceivedEvent = new Event<DuplexChannelMessageEventArgs>(myResponseMessageReceivedEventImpl);

    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private Event<DuplexChannelEventArgs> myConnectionOpenedEvent = new Event<DuplexChannelEventArgs>(myConnectionOpenedEventImpl);
    
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private Event<DuplexChannelEventArgs> myConnectionClosedEvent = new Event<DuplexChannelEventArgs>(myConnectionClosedEventImpl);
    
    
    private IMethod2<Object, ChannelMessageEventArgs> myResponseMessageReceivedHandler = new IMethod2<Object, ChannelMessageEventArgs>()
    {
        @Override
        public void invoke(Object t1, ChannelMessageEventArgs t2)
        {
            onResponseMessageReceived(t1, t2);
        }
    };
    

    private String TracedObject()
    {
        return "The duplex output channel '" + getChannelId() + "' ";
    }
}

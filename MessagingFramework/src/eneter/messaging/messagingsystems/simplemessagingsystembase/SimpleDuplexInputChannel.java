package eneter.messaging.messagingsystems.simplemessagingsystembase;

import java.security.InvalidParameterException;

import eneter.messaging.dataprocessing.streaming.MessageStreamer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;


public class SimpleDuplexInputChannel implements IDuplexInputChannel
{
    public SimpleDuplexInputChannel(String channelId, IMessagingSystemFactory messagingFactory)
    {
        if (channelId == null || channelId == "")
        {
            EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
            throw new InvalidParameterException(ErrorHandler.NullOrEmptyChannelId);
        }
        
        myDuplexInputChannelId = channelId;
        myMessagingSystemFactory = messagingFactory;
    }
    
    
    @Override
    public Event<DuplexChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEvent;
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEvent;
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEvent;
    }

    @Override
    public String getChannelId()
    {
        return myDuplexInputChannelId;
    }

    @Override
    public void startListening()
        throws Exception
    {
        synchronized (myListeningManipulatorLock)
        {
            if (isListening())
            {
                String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                EneterTrace.error(aMessage);
                throw new IllegalStateException(aMessage);
            }
            
            try
            {
                myMessageReceiverInputChannel = myMessagingSystemFactory.createInputChannel(myDuplexInputChannelId);
                myMessageReceiverInputChannel.messageReceived().subscribe(myMessageReceivedHandler);
                myMessageReceiverInputChannel.startListening();
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                stopListening();
                
                throw err;
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                stopListening();
                
                throw err;
            }
        }
        
    }

    @Override
    public void stopListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            if (myMessageReceiverInputChannel != null)
            {
                try
                {
                    myMessageReceiverInputChannel.stopListening();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.StopListeningFailure, err);
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StopListeningFailure, err);
                    throw err;
                }
                
                myMessageReceiverInputChannel.messageReceived().unsubscribe(myMessageReceivedHandler);
            }
        }
        
    }

    @Override
    public Boolean isListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            return myMessageReceiverInputChannel != null && myMessageReceiverInputChannel.isListening();
        }
    }

    @Override
    public void sendResponseMessage(String responseReceiverId, Object message)
            throws Exception
    {
        try
        {
            IOutputChannel aResponseOutputChannel = myMessagingSystemFactory.createOutputChannel(responseReceiverId);
            aResponseOutputChannel.sendMessage(message);
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);

            // Sending the response message failed, therefore consider it as the disconnection with the reponse receiver.
            notifyResponseReceiverDisconnected(responseReceiverId);

            throw err;
        }
        catch (Error err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);

            // Sending the response message failed, therefore consider it as the disconnection with the reponse receiver.
            notifyResponseReceiverDisconnected(responseReceiverId);

            throw err;
        }
    }

    @Override
    public void disconnectResponseReceiver(String responseReceiverId)
    {
        try
        {
            IOutputChannel aResponseOutputChannel = myMessagingSystemFactory.createOutputChannel(responseReceiverId);

            // TODO: Investigate how to transfer OpenConnection, CloseConnection requests!!! So that they can be read by .NET application.
            
            //Notify the response receiver about the disconnection.
            Object[] aCloseConnectionMessage = MessageStreamer.getCloseConnectionMessage(responseReceiverId);
            aResponseOutputChannel.sendMessage(aCloseConnectionMessage);
        }
        catch (Exception err)
        {
            EneterTrace.warning(TracedObject() + ErrorHandler.DisconnectResponseReceiverFailure + responseReceiverId, err);
        }
        catch (Error err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.DisconnectResponseReceiverFailure + responseReceiverId, err);
            throw err;
        }
        
    }

    
    private void onMessageReceived(Object o, ChannelMessageEventArgs e)
    {
        try
        {
            Object[] aMessage = (Object[])e.getMessage();

            if (MessageStreamer.isOpenConnectionMessage(aMessage))
            {
                notifyResponseReceiverConnected((String)aMessage[1]);
            }
            else if (MessageStreamer.isCloseConnectionMessage(aMessage))
            {
                notifyResponseReceiverDisconnected((String)aMessage[1]);
            }
            else if (MessageStreamer.isRequestMessage(aMessage))
            {
                notifyMessageReceived(getChannelId(), aMessage[2], (String)aMessage[1]);
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
        catch (Error err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.ReceiveMessageFailure, err);
            throw err;
        }
    }
    
    private void notifyResponseReceiverConnected(String responseReceiverId)
    {
        if (!myResponseReceiverConnectedEventImpl.isEmpty())
        {
            ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId);

            try
            {
                myResponseReceiverConnectedEventImpl.update(this, aResponseReceiverEvent);
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
    
    private void notifyResponseReceiverDisconnected(String responseReceiverId)
    {
        if (!myResponseReceiverDisconnectedEventImpl.isEmpty())
        {
            ResponseReceiverEventArgs aResponseReceiverEvent = new ResponseReceiverEventArgs(responseReceiverId);

            try
            {
                myResponseReceiverDisconnectedEventImpl.update(this, aResponseReceiverEvent);
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
    
    private void notifyMessageReceived(String channelId, Object message, String responseReceiverId)
    {
        if (!myMessageReceivedEventImpl.isEmpty())
        {
            try
            {
                myMessageReceivedEventImpl.update(this, new DuplexChannelMessageEventArgs(channelId, message, responseReceiverId));
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
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private Event<DuplexChannelMessageEventArgs> myMessageReceivedEvent = new Event<DuplexChannelMessageEventArgs>(myMessageReceivedEventImpl);
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private Event<ResponseReceiverEventArgs> myResponseReceiverConnectedEvent = new Event<ResponseReceiverEventArgs>(myResponseReceiverConnectedEventImpl);
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private Event<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEvent = new Event<ResponseReceiverEventArgs>(myResponseReceiverDisconnectedEventImpl);
    
    private IMessagingSystemFactory myMessagingSystemFactory;
    private IInputChannel myMessageReceiverInputChannel;
    private Object myListeningManipulatorLock = new Object();
    private String myDuplexInputChannelId = "";
    
    private IMethod2<Object, ChannelMessageEventArgs> myMessageReceivedHandler = new IMethod2<Object, ChannelMessageEventArgs>()
    {
        @Override
        public void invoke(Object t1, ChannelMessageEventArgs t2)
        {
            onMessageReceived(t1, t2);
        }
    };
    
    
    private String TracedObject()
    {
        return "The duplex input channel '" + myDuplexInputChannelId + "' ";
    }
}

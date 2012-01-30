package eneter.messaging.messagingsystems.simplemessagingsystembase;

import java.security.InvalidParameterException;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;


public class SimpleDuplexInputChannel implements IDuplexInputChannel
{
    public SimpleDuplexInputChannel(String channelId, IMessagingSystemFactory messagingFactory,
                                    IProtocolFormatter<?> protocolFormatter)
    {
        if (StringExt.isNullOrEmpty(channelId))
        {
            EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
            throw new InvalidParameterException(ErrorHandler.NullOrEmptyChannelId);
        }
        
        myDuplexInputChannelId = channelId;
        myMessagingSystemFactory = messagingFactory;
        myProtocolFormatter = protocolFormatter;
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
    public boolean isListening() throws Exception
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
            
            // Encode the response message.
            Object anEncodedMessage = myProtocolFormatter.encodeMessage("", message);
            
            aResponseOutputChannel.sendMessage(anEncodedMessage);
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

            // Encode the message for closing the connection with the client.
            Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(responseReceiverId);
            
            aResponseOutputChannel.sendMessage(anEncodedMessage);
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
            // Decode the incoming message.
            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(e.getMessage());

            if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
            {
                notifyResponseReceiverConnected(aProtocolMessage.ResponseReceiverId);
            }
            else if (aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                notifyResponseReceiverDisconnected(aProtocolMessage.ResponseReceiverId);
            }
            else if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
            {
                notifyMessageReceived(getChannelId(), aProtocolMessage.Message, aProtocolMessage.ResponseReceiverId);
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
    
    private void notifyResponseReceiverDisconnected(String responseReceiverId)
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
    
    private void notifyMessageReceived(String channelId, Object message, String responseReceiverId)
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
    
    
    private IProtocolFormatter<?> myProtocolFormatter;
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
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

package eneter.messaging.messagingsystems.simplemessagingsystembase;

import java.security.InvalidParameterException;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;


public class SimpleDuplexInputChannel implements IDuplexInputChannel
{
    public SimpleDuplexInputChannel(String channelId, IMessagingSystemFactory messagingFactory)
    {
        if (channelId == null || channelId == "")
        {
            // TODO: Trace
            //EneterTrace.Error(ErrorHandler.NullOrEmptyChannelId);
            throw new InvalidParameterException("Input parameter channelId is null or empty string.");
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
    {
        synchronized (myListeningManipulatorLock)
        {
            if (isListening())
            {
                // TODO: Trace.
                //string aMessage = TracedObject + ErrorHandler.IsAlreadyListening;
                //EneterTrace.Error(aMessage);
                throw new IllegalStateException("The duplex input channel is already listening.");
            }
            
            try
            {
                myMessageReceiverInputChannel = myMessagingSystemFactory.createInputChannel(myDuplexInputChannelId);
                myMessageReceiverInputChannel.messageReceived().subscribe(myMessageReceivedHandler);
                myMessageReceiverInputChannel.startListening();
            }
            catch (RuntimeException err)
            {
                // TODO: Trace
                
                try
                {
                    stopListening();
                }
                catch (Exception err2)
                {
                    
                }
                
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
                    // TODO: Trace warning.
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
    {
        try
        {
            IOutputChannel aResponseOutputChannel = myMessagingSystemFactory.createOutputChannel(responseReceiverId);
            aResponseOutputChannel.sendMessage(message);
        }
        catch (RuntimeException err)
        {
            // TODO: Error trace
            //EneterTrace.Error(TracedObject + ErrorHandler.SendResponseFailure, err);

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

            // TODO: Investigate how to transfer OpenConnection, CloseConnection requests!!!
            // Notify the response receiver about the disconnection.
            //Object[] aCloseConnectionMessage = MessageStreamer.GetCloseConnectionMessage(responseReceiverId);
            //aResponseOutputChannel.SendMessage(aCloseConnectionMessage);
        }
        catch (Exception err)
        {
            // TODO: trace warning.
            //EneterTrace.Warning(TracedObject + ErrorHandler.DisconnectResponseReceiverFailure + responseReceiverId, err);
        }
        
    }

    
    private void onMessageReceived(Object o, ChannelMessageEventArgs e)
    {
        
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
                // TODO: Trace warning.
                //EneterTrace.Warning(TracedObject + ErrorHandler.DetectedException, err);
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
                // TODO: Trace warning.
                //EneterTrace.Warning(TracedObject + ErrorHandler.DetectedException, err);
            }
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
}

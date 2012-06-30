package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.Event;
import eneter.net.system.EventImpl;
import eneter.net.system.EventHandler;

class Mock_MonitorDuplexInputChannel implements IDuplexInputChannel
{
    public Mock_MonitorDuplexInputChannel(IDuplexInputChannel underlyingInputChannel, ISerializer serializer)
    {
        myUnderlyingInputChannel = underlyingInputChannel;
        myUnderlyingInputChannel.responseReceiverConnected().subscribe(myOnResponseReceiverConnected);
        myUnderlyingInputChannel.responseReceiverDisconnected().subscribe(myOnResponseReceiverDisconnected);
        myUnderlyingInputChannel.messageReceived().subscribe(myOnMessageReceived);

        mySerializer = serializer;

        myResponsePingFlag = true;
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
        return myUnderlyingInputChannel.getChannelId();
    }

    @Override
    public void startListening() throws Exception
    {
        myUnderlyingInputChannel.startListening();
    }

    @Override
    public void stopListening()
    {
        myUnderlyingInputChannel.stopListening();
    }

    @Override
    public boolean isListening() throws Exception
    {
        return myUnderlyingInputChannel.isListening();
    }

    @Override
    public void sendResponseMessage(String responseReceiverId, Object message)
            throws Exception
    {
        // Create the response message for the monitor duplex output chanel.
        MonitorChannelMessage aMessage = new MonitorChannelMessage(MonitorChannelMessageType.Message, message);
        Object aSerializedMessage = mySerializer.serialize(aMessage, MonitorChannelMessage.class);

        // Send the response message via the underlying channel.
        myUnderlyingInputChannel.sendResponseMessage(responseReceiverId, aSerializedMessage);
    }

    @Override
    public void disconnectResponseReceiver(String responseReceiverId)
            throws Exception
    {
        myUnderlyingInputChannel.disconnectResponseReceiver(responseReceiverId);
    }
    
    private void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        if (myResponseReceiverConnectedEventImpl != null)
        {
            try
            {
                myResponseReceiverConnectedEventImpl.raise(this, e);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "detected an exception from the 'ResponseReceiverConnected' event handler.", err);
            }
        }
    }

    private void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        if (myResponseReceiverDisconnectedEventImpl.isSubscribed())
        {
            try
            {
                myResponseReceiverDisconnectedEventImpl.raise(this, e);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "detected an exception from the 'ResponseReceiverDisconnected' event handler.", err);
            }
        }
    }
    
    private void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        try
        {
            // Deserialize the incoming message.
            MonitorChannelMessage aMessage = mySerializer.deserialize(e.getMessage(), MonitorChannelMessage.class);

            // if the message is ping, then response.
            if (aMessage.MessageType == MonitorChannelMessageType.Ping)
            {
                EneterTrace.info(TracedObject() + "received the ping.");

                try
                {
                    if (myResponsePingFlag)
                    {
                        myUnderlyingInputChannel.sendResponseMessage(e.getResponseReceiverId(), e.getMessage());

                        EneterTrace.info(TracedObject() + "responded the ping.");
                    }
                    else
                    {
                        EneterTrace.info(TracedObject() + "did not response the ping.");
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to response to ping message.", err.getMessage());
                }
            }
            else
            {
                // Notify the incoming message.
                if (myMessageReceivedEventImpl.isSubscribed())
                {
                    DuplexChannelMessageEventArgs aMsg = new DuplexChannelMessageEventArgs(e.getChannelId(), aMessage.MessageContent, e.getResponseReceiverId());

                    try
                    {
                        myMessageReceivedEventImpl.raise(this, aMsg);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "detected an exception from the 'MessageReceived' event handler.", err);
                    }
                }
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + "failed to receive the message.", err.getMessage());
        }
    }
    
    
    private IDuplexInputChannel myUnderlyingInputChannel;
    private ISerializer mySerializer;
    
    public boolean myResponsePingFlag;
    
    
    private EventImpl<DuplexChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    
    private EventHandler<ResponseReceiverEventArgs> myOnResponseReceiverConnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object x, ResponseReceiverEventArgs y)
        {
            onResponseReceiverConnected(x, y);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myOnResponseReceiverDisconnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object x, ResponseReceiverEventArgs y)
        {
            onResponseReceiverDisconnected(x, y);
        }
    };
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object x, DuplexChannelMessageEventArgs y)
        {
            onMessageReceived(x, y);
        }
    };
    
    
    
    private String TracedObject()
    {
        String aChannelId = (myUnderlyingInputChannel != null) ? myUnderlyingInputChannel.getChannelId() : "";
        return "The MOCK monitor duplex input channel '" + aChannelId + "' ";
    }
}

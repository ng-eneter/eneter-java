package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.*;


class SyncTypedMessageReceiver<TResponse, TRequest> implements ISyncTypedMessageReceiver<TResponse, TRequest>
{
    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEvent.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEvent.getApi();
    }

    
    public SyncTypedMessageReceiver(IFunction1<TResponse, TypedRequestReceivedEventArgs<TRequest>> requestHandler, ISerializer serializer,
            Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IDuplexTypedMessagesFactory aReceiverFactory = new DuplexTypedMessagesFactory(serializer);
            myReceiver = aReceiverFactory.createDuplexTypedMessageReceiver(responseMessageClazz, requestMessageClazz);
            myReceiver.responseReceiverConnected().subscribe(myResponseReceiverConnectedEventHandler);
            myReceiver.responseReceiverDisconnected().subscribe(myResponseReceiverDisconnectedEventHandler);
            myReceiver.messageReceived().subscribe(myRequestMessageReceivedEventHandler);

            myRequestHandler = requestHandler;
            
            myResponseMessageClazz = responseMessageClazz;
            myRequestMessageClazz = requestMessageClazz;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void attachDuplexInputChannel(IDuplexInputChannel duplexInputChannel)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myReceiver.attachDuplexInputChannel(duplexInputChannel);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachDuplexInputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myReceiver.detachDuplexInputChannel();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isDuplexInputChannelAttached()
    {
        return myReceiver.isDuplexInputChannelAttached();
    }

    @Override
    public IDuplexInputChannel getAttachedDuplexInputChannel()
    {
        return myReceiver.getAttachedDuplexInputChannel();
    }

    
    private void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notifyConnectionStatus(myResponseReceiverConnectedEvent, e);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
   
    private void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notifyConnectionStatus(myResponseReceiverDisconnectedEvent, e);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onMessageReceived(Object sender, TypedRequestReceivedEventArgs<TRequest> e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TResponse aResponse;
            
            try
            {
                // Call user provided handler to get the response.
                aResponse = myRequestHandler.invoke(e);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                return;
            }

            try
            {
                // Send back the response.
                myReceiver.sendResponseMessage(e.getResponseReceiverId(), aResponse);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyConnectionStatus(EventImpl<ResponseReceiverEventArgs> handler, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler.isSubscribed())
            {
                try
                {
                    handler.raise(this, e);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IFunction1<TResponse, TypedRequestReceivedEventArgs<TRequest>> myRequestHandler;
    private IDuplexTypedMessageReceiver<TResponse, TRequest> myReceiver;
    
    private Class<TRequest> myRequestMessageClazz;
    private Class<TResponse> myResponseMessageClazz;
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    
    private EventHandler<ResponseReceiverEventArgs> myResponseReceiverConnectedEventHandler = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverConnected(sender, e);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventHandler = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverDisconnected(sender, e);
        }
    };
    
    private EventHandler<TypedRequestReceivedEventArgs<TRequest>> myRequestMessageReceivedEventHandler = new EventHandler<TypedRequestReceivedEventArgs<TRequest>>()
    {
        @Override
        public void onEvent(Object sender, TypedRequestReceivedEventArgs<TRequest> e)
        {
            onMessageReceived(sender, e);
        }
    };
    
    
    private String TracedObject()
    {
        String aResponseMessageTypeName = (myResponseMessageClazz != null) ? myResponseMessageClazz.getSimpleName() : "...";
        String aRequestMessageTypeName = (myRequestMessageClazz != null) ? myRequestMessageClazz.getSimpleName() : "...";
        String aDuplexInputChannelId = (getAttachedDuplexInputChannel() != null) ? getAttachedDuplexInputChannel().getChannelId() : "";
        return "SyncTypedMessageReceiver<" + aResponseMessageTypeName + ", " + aRequestMessageTypeName + "> atached to the duplex input channel '" + aDuplexInputChannelId + "' ";
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.util.HashMap;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.IMethod4;

class MultiTypedMessageReceiver implements IMultiTypedMessageReceiver
{
    private class TMessageHandler
    {
        public TMessageHandler(Class<?> type, IMethod4<String, String, Object, Exception> eventInvoker)
        {
            Type = type;
            Invoke = eventInvoker;
        }

        private Class<?> Type;
        private IMethod4<String, String, Object, Exception> Invoke;
    }

   
    public MultiTypedMessageReceiver(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;

            IDuplexTypedMessagesFactory aFactory = new DuplexTypedMessagesFactory(serializer);
            myReceiver = aFactory.createDuplexTypedMessageReceiver(MultiTypedMessage.class, MultiTypedMessage.class);
            myReceiver.responseReceiverConnected().subscribe(myOnResponseReceiverConnected);
            myReceiver.responseReceiverDisconnected().subscribe(myOnResponseReceiverDisconnected);
            myReceiver.messageReceived().subscribe(myOnRequestMessageReceived);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedImpl.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedImpl.getApi();
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

    

    @Override
    public <T> void registerRequestMessageReceiver(final EventHandler<TypedRequestReceivedEventArgs<T>> handler, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler == null)
            {
                String anError = TracedObject() + "failed to register handler for message " + clazz.getSimpleName() + " because the input parameter handler is null.";
                EneterTrace.error(anError);
                throw new IllegalArgumentException(anError);
            }

            synchronized (myMessageHandlers)
            {
                TMessageHandler aMessageHandler = myMessageHandlers.get(clazz.getSimpleName());

                if (aMessageHandler != null)
                {
                    String anError = TracedObject() + "failed to register handler for message " + clazz.getSimpleName() + " because the handler for such class name is already registered.";
                    EneterTrace.error(anError);
                    throw new IllegalStateException(anError);
                }

                // Note: the invoking method must be cached for particular types because
                //       during deserialization the generic argument is not available and so it would not be possible
                //       to instantiate TypedRequestReceivedEventArgs<T>.
                IMethod4<String, String, Object, Exception> anEventInvoker = new IMethod4<String, String, Object, Exception>()
                {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void invoke(String responseReceiverId, String senderAddress, Object message, Exception receivingError)
                            throws Exception
                    {
                        TypedRequestReceivedEventArgs<T> anEvent;
                        if (receivingError == null)
                        {
                            anEvent = new TypedRequestReceivedEventArgs<T>(responseReceiverId, senderAddress, (T)message);
                        }
                        else
                        {
                            anEvent = new TypedRequestReceivedEventArgs<T>(responseReceiverId, senderAddress, receivingError);
                        }
                        handler.onEvent(this, anEvent);
                    }
                }; 

                myMessageHandlers.put(clazz.getSimpleName(), new TMessageHandler(clazz, anEventInvoker));
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <T> void unregisterRequestMessageReceiver(Class<T> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myMessageHandlers)
            {
                myMessageHandlers.remove(clazz.getSimpleName());
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public Class<?>[] getRegisteredRequestMessageTypes()
    {
        synchronized (myMessageHandlers)
        {
            Class<?>[] aRegisteredMessageTypes = new Class<?>[myMessageHandlers.size()];
            int i = 0;
            for (TMessageHandler aHandler : myMessageHandlers.values())
            {
                aRegisteredMessageTypes[i] = aHandler.Type;
                ++i;
            }
            return aRegisteredMessageTypes;
        }
    }

    @Override
    public <TResponseMessage> void sendResponseMessage(String responseReceiverId, TResponseMessage responseMessage, Class<TResponseMessage> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            MultiTypedMessage aMessage = new MultiTypedMessage();
            aMessage.TypeName = clazz.getSimpleName();
            aMessage.MessageData = mySerializer.serialize(responseMessage, clazz);

            myReceiver.sendResponseMessage(responseReceiverId, aMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    
    private void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverConnectedImpl.isSubscribed())
            {
                try
                {
                    myResponseReceiverConnectedImpl.raise(this, e);
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
    
    private void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverDisconnectedImpl.isSubscribed())
            {
                try
                {
                    myResponseReceiverDisconnectedImpl.raise(this, e);
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
    
    private void onRequestMessageReceived(Object sender, TypedRequestReceivedEventArgs<MultiTypedMessage> e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (e.getReceivingError() != null)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToReceiveMessage, e.getReceivingError());
            }
            else
            {
                TMessageHandler aMessageHandler;

                synchronized (myMessageHandlers)
                {
                    aMessageHandler = myMessageHandlers.get(e.getRequestMessage().TypeName);
                }

                if (aMessageHandler != null)
                {
                    Object aMessageData;
                    try
                    {
                        aMessageData = mySerializer.deserialize(e.getRequestMessage().MessageData, aMessageHandler.Type);

                        try
                        {
                            aMessageHandler.Invoke.invoke(e.getResponseReceiverId(), e.getSenderAddress(), aMessageData, null);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                        }
                    }
                    catch (Exception err)
                    {
                        try
                        {
                            aMessageHandler.Invoke.invoke(e.getResponseReceiverId(), e.getSenderAddress(), null, err);
                        }
                        catch (Exception err2)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err2);
                        }
                    }
                }
                else
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage + " Message type = " + e.getRequestMessage().TypeName);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private ISerializer mySerializer;
    private IDuplexTypedMessageReceiver<MultiTypedMessage, MultiTypedMessage> myReceiver;
    
    private HashMap<String, TMessageHandler> myMessageHandlers = new HashMap<String, TMessageHandler>();
    
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    private EventHandler<ResponseReceiverEventArgs> myOnResponseReceiverConnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverConnected(sender, e);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myOnResponseReceiverDisconnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverDisconnected(sender, e);
        }
    };
    
    private EventHandler<TypedRequestReceivedEventArgs<MultiTypedMessage>> myOnRequestMessageReceived = new EventHandler<TypedRequestReceivedEventArgs<MultiTypedMessage>>()
    {
        @Override
        public void onEvent(Object sender, TypedRequestReceivedEventArgs<MultiTypedMessage> e)
        {
            onRequestMessageReceived(sender, e);
        }
    };
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

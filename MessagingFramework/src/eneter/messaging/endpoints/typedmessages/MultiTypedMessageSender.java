/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */


package eneter.messaging.endpoints.typedmessages;

import java.util.ArrayList;
import java.util.HashMap;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.*;



class MultiTypedMessageSender implements IMultiTypedMessageSender
{
    private class TMessageHandler
    {
        public TMessageHandler(Class<?> type, IMethod2<Object, Exception> eventInvoker)
        {
            Type = type;
            Invoke = eventInvoker;
        }

        public final Class<?> Type;
        public final IMethod2<Object, Exception> Invoke;
    }
    
    
    public MultiTypedMessageSender(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;

            IDuplexTypedMessagesFactory aFactory = new DuplexTypedMessagesFactory(serializer);
            mySender = aFactory.createDuplexTypedMessageSender(MultiTypedMessage.class, MultiTypedMessage.class);
            mySender.connectionOpened().subscribe(myOnConnectionOpened);
            mySender.connectionClosed().subscribe(myOnConnectionClosed);
            mySender.responseReceived().subscribe(myOnResponseReceived);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpened.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosed.getApi();
    }
    

    @Override
    public void attachDuplexOutputChannel(IDuplexOutputChannel duplexOutputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySender.attachDuplexOutputChannel(duplexOutputChannel);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachDuplexOutputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySender.detachDuplexOutputChannel();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isDuplexOutputChannelAttached()
    {
        return mySender.isDuplexOutputChannelAttached();
    }

    @Override
    public IDuplexOutputChannel getAttachedDuplexOutputChannel()
    {
        return mySender.getAttachedDuplexOutputChannel();
    }

    @Override
    public <T> void registerResponseMessageReceiver(final EventHandler<TypedResponseReceivedEventArgs<T>> handler, Class<T> clazz)
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
            
            myMessageHandlersLock.lock();
            try
            {
                String aNetTypeName = MultiTypeNameProvider.getNetName(clazz);
                
                TMessageHandler aMessageHandler = myMessageHandlers.get(aNetTypeName);

                if (aMessageHandler != null)
                {
                    String anError = TracedObject() + "failed to register handler for message " + aNetTypeName + " because the handler for such class name is already registered.";
                    EneterTrace.error(anError);
                    throw new IllegalStateException(anError);
                }

                // Note: the invoking method must be cached for particular types because
                //       during deserialization the generic argument is not available and so it would not be possible
                //       to instantiate TypedRequestReceivedEventArgs<T>.
                final Object anEventSender = this;
                IMethod2<Object, Exception> anEventInvoker = new IMethod2<Object, Exception>()
                {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void invoke(Object message, Exception receivingError)
                            throws Exception
                    {
                        TypedResponseReceivedEventArgs<T> anEvent;
                        if (receivingError == null)
                        {
                            anEvent = new TypedResponseReceivedEventArgs<T>((T)message);
                        }
                        else
                        {
                            anEvent = new TypedResponseReceivedEventArgs<T>(receivingError);
                        }
                        handler.onEvent(anEventSender, anEvent);
                    }
                }; 

                myMessageHandlers.put(aNetTypeName, new TMessageHandler(clazz, anEventInvoker));
            }
            finally
            {
                myMessageHandlersLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <T> void unregisterResponseMessageReceiver(Class<T> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessageHandlersLock.lock();
            try
            {
                myMessageHandlers.remove(MultiTypeNameProvider.getNetName(clazz));
            }
            finally
            {
                myMessageHandlersLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public ArrayList<Class<?>> getRegisteredResponseMessageTypes()
    {
        myMessageHandlersLock.lock();
        try
        {
            ArrayList<Class<?>> aRegisteredMessageTypes = new ArrayList<Class<?>>();
            for (TMessageHandler aHandler : myMessageHandlers.values())
            {
                aRegisteredMessageTypes.add(aHandler.Type);
            }
            return aRegisteredMessageTypes;
        }
        finally
        {
            myMessageHandlersLock.unlock();
        }
    }

    @Override
    public <TRequestMessage> void sendRequestMessage(TRequestMessage message, Class<TRequestMessage> clazz) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                MultiTypedMessage aMessage = new MultiTypedMessage();
                aMessage.TypeName = MultiTypeNameProvider.getNetName(clazz);
                aMessage.MessageData = mySerializer.serialize(message, clazz);

                mySender.sendRequestMessage(aMessage);
            }
            catch (Exception err)
            {
                String anErrorMessage = TracedObject() + ErrorHandler.FailedToSendMessage;
                EneterTrace.error(anErrorMessage, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private void onConnectionOpened(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myConnectionOpened.isSubscribed())
            {
                try
                {
                    myConnectionOpened.raise(this, e);
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
    
    private void onConnectionClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myConnectionClosed.isSubscribed())
            {
                try
                {
                    myConnectionClosed.raise(this, e);
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
    
    private void onResponseReceived(Object sender, TypedResponseReceivedEventArgs<MultiTypedMessage> e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (e.getReceivingError() == null)
            {
                TMessageHandler aMessageHandler;

                myMessageHandlersLock.lock();
                try
                {
                    aMessageHandler = myMessageHandlers.get(e.getResponseMessage().TypeName);
                }
                finally
                {
                    myMessageHandlersLock.unlock();
                }

                if (aMessageHandler != null)
                {
                    Object aMessageData;
                    try
                    {
                        aMessageData = mySerializer.deserialize(e.getResponseMessage().MessageData, aMessageHandler.Type);

                        try
                        {
                            aMessageHandler.Invoke.invoke(aMessageData, null);
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
                            aMessageHandler.Invoke.invoke(null, err);
                        }
                        catch (Exception err2)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err2);
                        }
                    }

                }
                else
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage + " Message type = " + e.getResponseMessage().TypeName);
                }
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToReceiveMessage, e.getReceivingError());
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private ISerializer mySerializer;
    private IDuplexTypedMessageSender<MultiTypedMessage, MultiTypedMessage> mySender;

    private ThreadLock myMessageHandlersLock = new ThreadLock(); 
    private HashMap<String, TMessageHandler> myMessageHandlers = new HashMap<String, TMessageHandler>();
    
    
    private EventImpl<DuplexChannelEventArgs> myConnectionOpened = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosed = new EventImpl<DuplexChannelEventArgs>();
    
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionOpened = new EventHandler<DuplexChannelEventArgs>()
    {
             @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionOpened(sender, e);
        }
    }; 
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionClosed = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionClosed(sender, e);
        }
    };
    
    private EventHandler<TypedResponseReceivedEventArgs<MultiTypedMessage>> myOnResponseReceived = new EventHandler<TypedResponseReceivedEventArgs<MultiTypedMessage>>()
    {
        @Override
        public void onEvent(Object sender, TypedResponseReceivedEventArgs<MultiTypedMessage> e)
        {
            onResponseReceived(sender, e);
        }
    };
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

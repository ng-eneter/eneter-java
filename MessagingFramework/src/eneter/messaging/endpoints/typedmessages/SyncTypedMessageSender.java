/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.util.ArrayList;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.threading.internal.ManualResetEvent;

class SyncTypedMessageSender<TResponse, TRequest> implements ISyncDuplexTypedMessageSender<TResponse, TRequest>
{
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
    
    public SyncTypedMessageSender(int responseReceiveTimeout, ISerializer serializer,
            Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz,
            IThreadDispatcher threadDispatcher)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myResponseReceiveTimeout = responseReceiveTimeout;

            IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory(serializer);
            mySender = aSenderFactory.createDuplexTypedMessageSender(responseMessageClazz, requestMessageClazz);
            mySender.getAttachedDuplexOutputChannel().connectionClosed().subscribe(myOnConnectionOpened);
            mySender.getAttachedDuplexOutputChannel().connectionClosed().subscribe(myOnConnectionClosed);
            
            myResponseMessageClazz = responseMessageClazz;
            myRequestMessageClazz = requestMessageClazz;
            
            myThreadDispatcher = threadDispatcher;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void attachDuplexOutputChannel(
            IDuplexOutputChannel duplexOutputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myAttachDetachLock)
            {
                mySender.attachDuplexOutputChannel(duplexOutputChannel);
            }
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
            synchronized (myAttachDetachLock)
            {
                // Stop waiting for the response.
                myResponseAvailableEvent.set();
                
                mySender.detachDuplexOutputChannel();
            }
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
    public TResponse sendRequestMessage(TRequest message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // During sending and receiving only one caller is allowed.
            synchronized (myRequestResponseLock)
            {
                final ArrayList<TypedResponseReceivedEventArgs<TResponse>> aReceivedResponse = new ArrayList<TypedResponseReceivedEventArgs<TResponse>>();
                
                EventHandler<TypedResponseReceivedEventArgs<TResponse>> aResponseHandler = new EventHandler<TypedResponseReceivedEventArgs<TResponse>>()
                {
                    @Override
                    public void onEvent(Object x, TypedResponseReceivedEventArgs<TResponse> y)
                    {
                        aReceivedResponse.add(y);
                        myResponseAvailableEvent.set();
                    }
                }; 
                        
                mySender.responseReceived().subscribe(aResponseHandler);

                try
                {
                    myResponseAvailableEvent.reset();
                    mySender.sendRequestMessage(message);

                    // Wait auntil the response is received or the waiting was interrupted or timeout.
                    // Note: use int instead of TimeSpan due to compatibility reasons. E.g. Compact Framework does not support TimeSpan in WaitOne().
                    if (!myResponseAvailableEvent.waitOne(myResponseReceiveTimeout))
                    {
                        String anErrorMessage = TracedObject() + "failed to receive the response with the timeout. " + myResponseReceiveTimeout;
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }

                    // If response data does not exist.
                    if (aReceivedResponse.size() == 0)
                    {
                        String anErrorMessage = TracedObject() + "failed to receive the response.";

                        IDuplexOutputChannel anAttachedOutputChannel = mySender.getAttachedDuplexOutputChannel();
                        if (anAttachedOutputChannel == null)
                        {
                            anErrorMessage += " The duplex outputchannel was detached.";
                        }
                        else if (!anAttachedOutputChannel.isConnected())
                        {
                            anErrorMessage += " The connection was closed.";
                        }
                        
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }

                    // If an error occured during receving the response then throw exception.
                    Exception aReceivingError = aReceivedResponse.get(0).getReceivingError();
                    if (aReceivingError != null)
                    {
                        String anErrorMessage = TracedObject() + "failed to receive the response.";
                        EneterTrace.error(anErrorMessage, aReceivingError);
                        throw new IllegalStateException(anErrorMessage, aReceivingError);
                    }

                    return aReceivedResponse.get(0).getResponseMessage();
                }
                finally
                {
                    mySender.responseReceived().unsubscribe(aResponseHandler);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onConnectionOpened(Object sender, final DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myThreadDispatcher.invoke(new Runnable()
            {
                @Override
                public void run()
                {
                    notifyEvent(myConnectionOpenedEventImpl, e);
                }
            });
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onConnectionClosed(Object sender, final DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // The connection was interrupted therefore we must unblock the waiting request.
            myResponseAvailableEvent.set();
            
            myThreadDispatcher.invoke(new Runnable()
            {
                @Override
                public void run()
                {
                    notifyEvent(myConnectionClosedEventImpl, e);
                }
            });
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyEvent(EventImpl<DuplexChannelEventArgs> handler, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler != null)
            {
                try
                {
                    handler.raise(this, e);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private Object myAttachDetachLock = new Object();
    private Object myRequestResponseLock = new Object();

    private ManualResetEvent myResponseAvailableEvent = new ManualResetEvent(false);

    private int myResponseReceiveTimeout;
    private IDuplexTypedMessageSender<TResponse, TRequest> mySender;
    
    private IThreadDispatcher myThreadDispatcher;
    
    private Class<TRequest> myRequestMessageClazz;
    private Class<TResponse> myResponseMessageClazz;
    
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    
    
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
    
    
    private String TracedObject()
    {
        String aResponseMessageTypeName = (myResponseMessageClazz != null) ? myResponseMessageClazz.getSimpleName() : "...";
        String aRequestMessageTypeName = (myRequestMessageClazz != null) ? myRequestMessageClazz.getSimpleName() : "...";
        String aDuplexOutputChannelId = (getAttachedDuplexOutputChannel() != null) ? getAttachedDuplexOutputChannel().getChannelId() : "";
        return getClass().getSimpleName() + "<" + aResponseMessageTypeName + ", " + aRequestMessageTypeName + "> atached to the duplex output channel '" + aDuplexOutputChannelId + "' ";
    }
}

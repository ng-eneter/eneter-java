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
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.AutoResetEvent;

class SyncTypedMessageSender<TResponse, TRequest> implements ISyncDuplexTypedMessageSender<TResponse, TRequest>
{
    public SyncTypedMessageSender(int responseReceiveTimeout, ISerializer serializer,
            Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myResponseReceiveTimeout = responseReceiveTimeout;

            IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory(serializer);
            mySender = aSenderFactory.createDuplexTypedMessageSender(responseMessageClazz, requestMessageClazz);
            
            myResponseMessageClazz = responseMessageClazz;
            myRequestMessageClazz = requestMessageClazz;
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
                mySender.getAttachedDuplexOutputChannel().connectionClosed().subscribe(myConnectionClosedEventHandler);
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

                IDuplexOutputChannel anAttachedChannel = mySender.getAttachedDuplexOutputChannel();
                if (anAttachedChannel != null)
                {
                    anAttachedChannel.connectionClosed().unsubscribe(myConnectionClosedEventHandler);
                }

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
    
    private void onConnectionClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // The connection was interrupted therefore we must unblock the waiting request.
            myResponseAvailableEvent.set();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private Object myAttachDetachLock = new Object();
    private Object myRequestResponseLock = new Object();

    private AutoResetEvent myResponseAvailableEvent = new AutoResetEvent(false);

    private int myResponseReceiveTimeout;
    private IDuplexTypedMessageSender<TResponse, TRequest> mySender;
    
    private Class<TRequest> myRequestMessageClazz;
    private Class<TResponse> myResponseMessageClazz;
    
    private EventHandler<DuplexChannelEventArgs> myConnectionClosedEventHandler = new EventHandler<DuplexChannelEventArgs>()
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
        return "SyncTypedMessageSender<" + aResponseMessageTypeName + ", " + aRequestMessageTypeName + "> atached to the duplex output channel '" + aDuplexOutputChannelId + "' ";
    }
}

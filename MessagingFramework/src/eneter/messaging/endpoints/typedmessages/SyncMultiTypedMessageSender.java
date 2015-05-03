/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcherProvider;
import eneter.net.system.*;

class SyncMultiTypedMessageSender implements ISyncMultitypedMessageSender
{
    public SyncMultiTypedMessageSender(int syncResponseReceiveTimeout, ISerializer serializer, IThreadDispatcherProvider syncDuplexTypedSenderThreadMode)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
            DuplexTypedMessagesFactory aFactory = new DuplexTypedMessagesFactory(serializer)
                .setSyncResponseReceiveTimeout(syncResponseReceiveTimeout);
            aFactory.setSyncDuplexTypedSenderThreadMode(syncDuplexTypedSenderThreadMode);

            mySender = aFactory.createSyncDuplexTypedMessageSender(MultiTypedMessage.class, MultiTypedMessage.class);
            mySender.connectionOpened().subscribe(myOnConnectionOpened);
            mySender.connectionClosed().subscribe(myOnConnectionClosed);
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
    public void attachDuplexOutputChannel(
            IDuplexOutputChannel duplexOutputChannel) throws Exception
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
    public <TRequest, TResponse> TResponse sendRequestMessage(TRequest message, Class<TResponse> responseClazz, Class<TRequest> requestClazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                MultiTypedMessage aRequest = new MultiTypedMessage();
                aRequest.TypeName = MultiTypeNameProvider.getNetName(requestClazz);
                aRequest.MessageData = mySerializer.serialize(message, requestClazz);

                MultiTypedMessage aResponse = mySender.sendRequestMessage(aRequest);

                TResponse aResult = mySerializer.deserialize(aResponse.MessageData, responseClazz);
                return aResult;
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
    
    
    private ISerializer mySerializer;
    private ISyncDuplexTypedMessageSender<MultiTypedMessage, MultiTypedMessage> mySender;
    
    
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

    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

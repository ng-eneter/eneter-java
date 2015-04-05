/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.XmlStringSerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.threading.dispatching.IThreadDispatcherProvider;
import eneter.messaging.threading.dispatching.SyncDispatching;

public class MultiTypedMessagesFactory implements IMultiTypedMessagesFactory
{
    public MultiTypedMessagesFactory()
    {
        this(new XmlStringSerializer());
    }
    
    public MultiTypedMessagesFactory(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySyncResponseReceiveTimeout = 0;
            mySerializer = serializer;
            mySyncDuplexTypedSenderThreadMode = new SyncDispatching();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public IMultiTypedMessageSender createMultiTypedMessageSender()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new MultiTypedMessageSender(mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public ISyncMultitypedMessageSender createSyncMultiTypedMessageSender()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SyncMultiTypedMessageSender(mySyncResponseReceiveTimeout, mySerializer, mySyncDuplexTypedSenderThreadMode);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IMultiTypedMessageReceiver createMultiTypedMessageReceiver()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new MultiTypedMessageReceiver(mySerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    /**
     * Sets the threading mode for receiving connectionOpened and connectionClosed events for SyncDuplexTypedMessageSender.
     * 
     * E.g. you use SyncDuplexTypedMessageSender and you want to route ConnectionOpened and ConnectionClosed events
     * to the main UI thread of your WPF based application. Therefore you specify WindowsDispatching when you create your
     * TCP duplex output channel which you then attach to the SyncDuplexTypedMessageSender.<br/>
     * Later when the application is running you call SyncDuplexTypedMessageSender.SendRequestMessage(..).<br/>
     * However if you call it from the main UI thread the deadlock occurs.
     * Because this component is synchronous the SendRequestMessage(..) will stop the calling main UI thread and will wait
     * for the response. But the problem is when the response comes the underlying TCP messaging will try to route it to
     * the main UI thread (as was specified during creating TCP duplex output channel).<br/>
     * But because the main UI thread is suspending and waiting the message will never arrive.<br/>
     * <br/>
     * Solution:<br/>
     * Do not specify the threading mode when you create yur duplex output channel but specify it using the
     * SyncDuplexTypedSenderThreadMode property when you create SyncDuplexTypedMessageSender.
     * 
     * @param threadingMode threading that shall be used for receiving connectionOpened and connectionClosed events.
     * @return instance of this DuplexTypedMessagesFactory
     */
    public MultiTypedMessagesFactory setSyncDuplexTypedSenderThreadMode(IThreadDispatcherProvider threadingMode)
    {
        mySyncDuplexTypedSenderThreadMode = threadingMode;
        return this;
    }
    
    /**
     * Gets the threading mode which is used for receiving connectionOpened and connectionClosed events in SyncDuplexTypedMessageSender.
     * @return
     */
    public IThreadDispatcherProvider getSyncDuplexTypedSenderThreadMode()
    {
        return mySyncDuplexTypedSenderThreadMode;
    }
    
    public MultiTypedMessagesFactory setSerializer(ISerializer serializer)
    {
        mySerializer = serializer;
        return this;
    }
    
    public ISerializer getSerializer()
    {
        return mySerializer;
    }
    
    public MultiTypedMessagesFactory setSyncResponseReceiveTimeout(int milliseconds)
    {
        mySyncResponseReceiveTimeout = milliseconds;
        return this;
    }
    
    public int getSyncResponseReceiveTimeout()
    {
        return mySyncResponseReceiveTimeout;
    }
    
    
    private ISerializer mySerializer;
    private int mySyncResponseReceiveTimeout;
    private IThreadDispatcherProvider mySyncDuplexTypedSenderThreadMode;
}

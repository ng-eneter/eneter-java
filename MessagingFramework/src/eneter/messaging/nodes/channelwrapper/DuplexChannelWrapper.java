/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import java.util.*;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexOutputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelMessageEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;


class DuplexChannelWrapper extends AttachableDuplexOutputChannelBase
                           implements IDuplexChannelWrapper
{
    private class TDuplexInputChannel
    {
        public TDuplexInputChannel(IDuplexInputChannel duplexInputChannel)
        {
            myDuplexInputChannel = duplexInputChannel;
        }

        public IDuplexInputChannel getDuplexInputChannel()
        {
            return myDuplexInputChannel;
        }
        
        public String getResponseReceiverId()
        {
            return myResponseReceiverId;
        }
        
        public void setResponseReceiverId(String responseReceiverId)
        {
            myResponseReceiverId = responseReceiverId;
        }
        
        private IDuplexInputChannel myDuplexInputChannel;
        private String myResponseReceiverId;
    }
    
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
    
    public DuplexChannelWrapper(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
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
            myDuplexInputChannelsLock.lock();
            try
            {
                attach(duplexInputChannel);

                try
                {

                    duplexInputChannel.startListening();
                }
                catch (Exception err)
                {
                    // Try to clean after the failure
                    try
                    {
                        detachDuplexInputChannel(duplexInputChannel.getChannelId());
                    }
                    catch(Exception err2)
                    {
                    }

                    String aMessage = TracedObject() + "failed to start listening for '" + duplexInputChannel.getChannelId() + "'.";
                    EneterTrace.error(aMessage, err);
                    throw err;
                }
            }
            finally
            {
                myDuplexInputChannelsLock.unlock();
            }
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
            myDuplexInputChannelsLock.lock();
            try
            {
                for (TDuplexInputChannel anInputChannel : myDuplexInputChannels.values())
                {
                    anInputChannel.getDuplexInputChannel().stopListening();
                    anInputChannel.getDuplexInputChannel().messageReceived().unsubscribe(myOnMessageReceived);
                }

                myDuplexInputChannels.clear();
            }
            finally
            {
                myDuplexInputChannelsLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void detachDuplexInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDuplexInputChannelsLock.lock();
            try
            {
                TDuplexInputChannel anInputChannel = myDuplexInputChannels.get(channelId);
                if (anInputChannel != null)
                {
                    anInputChannel.getDuplexInputChannel().stopListening();
                    anInputChannel.getDuplexInputChannel().messageReceived().unsubscribe(myOnMessageReceived);
                }

                myDuplexInputChannels.remove(channelId);
            }
            finally
            {
                myDuplexInputChannelsLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isDuplexInputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDuplexInputChannelsLock.lock();
            try
            {
                return myDuplexInputChannels.size() > 0;
            }
            finally
            {
                myDuplexInputChannelsLock.lock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    @Override
    public Iterable<IDuplexInputChannel> getAttachedDuplexInputChannels()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: Because of thread safety, create a new container to store the references.
            ArrayList<IDuplexInputChannel> anAttachedChannels = new ArrayList<IDuplexInputChannel>();
            myDuplexInputChannelsLock.lock();
            try
            {
                for (TDuplexInputChannel aDuplexInputChannelItem : myDuplexInputChannels.values())
                {
                    anAttachedChannels.add(aDuplexInputChannelItem.getDuplexInputChannel());
                }
            }
            finally
            {
                myDuplexInputChannelsLock.unlock();
            }

            return anAttachedChannels;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (!isDuplexInputChannelAttached())
            {
                EneterTrace.error(TracedObject() + "is not attached to the duplex output channel.");
                return;
            }

            try
            {
                myDuplexInputChannelsLock.lock();
                try
                {
                    myDuplexInputChannels.get(e.getChannelId()).setResponseReceiverId(e.getResponseReceiverId());
                }
                finally
                {
                    myDuplexInputChannelsLock.unlock();
                }

                Object aMessage = DataWrapper.wrap(e.getChannelId(), e.getMessage(), mySerializer);
                getAttachedDuplexOutputChannel().sendMessage(aMessage);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to send the message to the duplex output channel '" + e.getChannelId() + "'.", err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                WrappedData aWrappedData = DataWrapper.unwrap(e.getMessage(), mySerializer);

                // WrappedData.AddedData represents the channel id.
                // Therefore if everything is ok then it must be string.
                if (aWrappedData.AddedData instanceof String)
                {
                    // Get the output channel according to the channel id.
                    TDuplexInputChannel aDuplexInputChannel = null;

                    myDuplexInputChannelsLock.lock();
                    try
                    {
                        aDuplexInputChannel = myDuplexInputChannels.get((String)aWrappedData.AddedData);
                    }
                    finally
                    {
                        myDuplexInputChannelsLock.unlock();
                    }

                    if (aDuplexInputChannel != null)
                    {
                        aDuplexInputChannel.getDuplexInputChannel().sendResponseMessage(aDuplexInputChannel.getResponseReceiverId(), aWrappedData.OriginalData);
                    }
                    else
                    {
                        EneterTrace.warning(TracedObject() + "could not send the response message to the duplex input channel '" + (String)aWrappedData.AddedData + "' because the channel is not attached to the unwrapper.");
                    }
                }
                else
                {
                    EneterTrace.error(TracedObject() + "detected that the unwrapped message does not contain the channel id as the string type.");
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to process the response message.", err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected void onConnectionOpened(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notify(myConnectionOpenedEventImpl, e);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void onConnectionClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notify(myConnectionClosedEventImpl, e);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void attach(IDuplexInputChannel duplexInputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDuplexInputChannelsLock.lock();
            try
            {
                if (duplexInputChannel == null)
                {
                    String aMessage = TracedObject() + "failed to attach duplex input channel because the input parameter 'duplexInputChannel' is null.";
                    EneterTrace.error(aMessage);
                    throw new IllegalArgumentException(aMessage);
                }

                if (StringExt.isNullOrEmpty(duplexInputChannel.getChannelId()))
                {
                    String aMessage = TracedObject() + "failed to attach duplex input channel because the input parameter 'duplexInputChannel' has empty or null channel id.";
                    EneterTrace.error(aMessage);
                    throw new IllegalArgumentException(aMessage);
                }

                if (myDuplexInputChannels.containsKey(duplexInputChannel.getChannelId()))
                {
                    String anErrorMessage = TracedObject() + "failed to attach duplex input channel because the channel with id '" + duplexInputChannel.getChannelId() + "' is already attached.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }

                myDuplexInputChannels.put(duplexInputChannel.getChannelId(), new TDuplexInputChannel(duplexInputChannel));

                duplexInputChannel.messageReceived().subscribe(myOnMessageReceived);
            }
            finally
            {
                myDuplexInputChannelsLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notify(EventImpl<DuplexChannelEventArgs> handler, DuplexChannelEventArgs e)
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
    
    private ThreadLock myDuplexInputChannelsLock = new ThreadLock();
    private HashMap<String, TDuplexInputChannel> myDuplexInputChannels = new HashMap<String, TDuplexInputChannel>();
    private ISerializer mySerializer;
    
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onMessageReceived(sender, e);
        }
    };
    

    @Override
    protected String TracedObject()
    {
        String aDuplexOutputChannelId = (getAttachedDuplexOutputChannel() != null) ? getAttachedDuplexOutputChannel().getChannelId() : "";
        return getClass().getSimpleName() + " '" + aDuplexOutputChannelId + "' "; 
    }
}

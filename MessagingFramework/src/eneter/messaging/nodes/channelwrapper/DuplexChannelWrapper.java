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
import eneter.messaging.dataprocessing.wrapping.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.infrastructure.attachable.AttachableDuplexOutputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelMessageEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.net.system.*;


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
            synchronized (myDuplexInputChannels)
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
                catch (Error err)
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
            synchronized (myDuplexInputChannels)
            {
                for (TDuplexInputChannel anInputChannel : myDuplexInputChannels.values())
                {
                    anInputChannel.getDuplexInputChannel().stopListening();
                    anInputChannel.getDuplexInputChannel().messageReceived().unsubscribe(myOnMessageReceivedEventHandler);
                }

                myDuplexInputChannels.clear();
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
            synchronized (myDuplexInputChannels)
            {
                TDuplexInputChannel anInputChannel = myDuplexInputChannels.get(channelId);
                if (anInputChannel != null)
                {
                    anInputChannel.getDuplexInputChannel().stopListening();
                    anInputChannel.getDuplexInputChannel().messageReceived().unsubscribe(myOnMessageReceivedEventHandler);
                }

                myDuplexInputChannels.remove(channelId);
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
            synchronized (myDuplexInputChannels)
            {
                return myDuplexInputChannels.size() > 0;
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
            synchronized (myDuplexInputChannels)
            {
                for (TDuplexInputChannel aDuplexInputChannelItem : myDuplexInputChannels.values())
                {
                    anAttachedChannels.add(aDuplexInputChannelItem.getDuplexInputChannel());
                }
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
                synchronized (myDuplexInputChannels)
                {
                    myDuplexInputChannels.get(e.getChannelId()).setResponseReceiverId(e.getResponseReceiverId());
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

                    synchronized (myDuplexInputChannels)
                    {
                        aDuplexInputChannel = myDuplexInputChannels.get((String)aWrappedData.AddedData);
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
    
    
    private void attach(IDuplexInputChannel duplexInputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexInputChannels)
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

                duplexInputChannel.messageReceived().subscribe(myOnMessageReceivedEventHandler);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    private Hashtable<String, TDuplexInputChannel> myDuplexInputChannels = new Hashtable<String, TDuplexInputChannel>();
    private ISerializer mySerializer;
    
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnMessageReceivedEventHandler = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void invoke(Object sender, DuplexChannelMessageEventArgs e)
                throws Exception
        {
            onMessageReceived(sender, e);
        }
    };
    

    @Override
    protected String TracedObject()
    {
        String aDuplexOutputChannelId = (getAttachedDuplexOutputChannel() != null) ? getAttachedDuplexOutputChannel().getChannelId() : "";
        return "The DuplexChannelWrapper attached to the duplex output channel '" + aDuplexOutputChannelId + "' "; 
    }

}

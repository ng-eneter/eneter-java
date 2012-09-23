/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.dataprocessing.wrapping.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.infrastructure.attachable.internal.AttachableOutputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;

import java.util.*;

class ChannelWrapper extends AttachableOutputChannelBase
                     implements IChannelWrapper
{
    public ChannelWrapper(ISerializer serializer)
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
    public void attachInputChannel(IInputChannel inputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                if (myInputChannels.containsKey(inputChannel.getChannelId()))
                {
                    String anErrorMessage = TracedObject() + "cannot attach the input channel because the input channel with the id '" + inputChannel.getChannelId() + "' is already attached.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }

                myInputChannels.put(inputChannel.getChannelId(), inputChannel);
                inputChannel.messageReceived().subscribe(myOnMessageReceivedHandler);

                try
                {
                    inputChannel.startListening();
                }
                catch (Exception err)
                {
                    inputChannel.messageReceived().unsubscribe(myOnMessageReceivedHandler);
                    myInputChannels.remove(inputChannel.getChannelId());

                    EneterTrace.error(TracedObject() + "failed to attach the input channel '" + inputChannel.getChannelId() + "'.", err);

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
    public void detachInputChannel(String inputChannelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                IInputChannel anInputChannel = myInputChannels.get(inputChannelId);
                if (anInputChannel != null)
                {
                    anInputChannel.stopListening();
                    anInputChannel.messageReceived().unsubscribe(myOnMessageReceivedHandler);
                }

                myInputChannels.remove(inputChannelId);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachInputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                for (IInputChannel anInputChannel : myInputChannels.values())
                {
                    anInputChannel.stopListening();
                    anInputChannel.messageReceived().unsubscribe(myOnMessageReceivedHandler);
                }

                myInputChannels.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isInputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                return myInputChannels.size() > 0;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public Iterable<IInputChannel> getAttachedInputChannels()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                return myInputChannels.values();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private void onMessageReceived(Object sender, ChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                Object aMessage = DataWrapper.wrap(e.getChannelId(), e.getMessage(), mySerializer);
                getAttachedOutputChannel().sendMessage(aMessage);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to send the wrapped message.", err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private Hashtable<String, IInputChannel> myInputChannels = new Hashtable<String, IInputChannel>();
    
    private ISerializer mySerializer;
    
    private EventHandler<ChannelMessageEventArgs> myOnMessageReceivedHandler = new EventHandler<ChannelMessageEventArgs>()
            {
                @Override
                public void onEvent(Object t1, ChannelMessageEventArgs t2)
                {
                    onMessageReceived(t1, t2);
                }
            };
    
    
    private String TracedObject()
    {
        String aDuplexOutputChannelId = (getAttachedOutputChannel() != null) ? getAttachedOutputChannel().getChannelId() : "";
        return "The ChannelWrapper attached to the output channel '" + aDuplexOutputChannelId + "' ";
    }
}

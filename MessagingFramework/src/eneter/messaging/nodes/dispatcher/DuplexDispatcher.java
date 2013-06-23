/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

import java.util.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableMultipleDuplexInputChannelsBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;


class DuplexDispatcher extends AttachableMultipleDuplexInputChannelsBase
                       implements IDuplexDispatcher
{
    public DuplexDispatcher(IMessagingSystemFactory duplexOutputChannelMessagingSystem)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            setMessagingSystemFactory(duplexOutputChannelMessagingSystem);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public void addDuplexOutputChannel(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexOutputChannelIds)
            {
                myDuplexOutputChannelIds.add(channelId);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void removeDuplexOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexOutputChannelIds)
            {
                myDuplexOutputChannelIds.remove(channelId);
                closeDuplexOutputChannel(channelId);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void removeAllDuplexOutputChannels() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexOutputChannelIds)
            {
                try
                {
                    for (String aDuplexOutputChannelId : myDuplexOutputChannelIds)
                    {
                        closeDuplexOutputChannel(aDuplexOutputChannelId);
                    }
                }
                finally
                {
                    myDuplexOutputChannelIds.clear();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void onMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myDuplexOutputChannelIds)
            {
                for (String aDuplexOutputChannelId : myDuplexOutputChannelIds)
                {
                    try
                    {
                        sendMessage(e.getChannelId(), e.getResponseReceiverId(), aDuplexOutputChannelId, e.getMessage());
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                    }
                }
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
                sendResponseMessage(e.getResponseReceiverId(), e.getMessage());
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    
    private ArrayList<String> myDuplexOutputChannelIds = new ArrayList<String>();
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase;

import java.security.InvalidParameterException;

import eneter.messaging.messagingsystems.messagingsystembase.ChannelMessageEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IInputChannel;
import eneter.net.system.Event;
import eneter.net.system.EventImpl;
import eneter.net.system.IMethod1;

public class SimpleInputChannel implements IInputChannel
{
    public SimpleInputChannel(String channelId, IMessagingSystemBase messagingSystem)
    {
        if (channelId == null || channelId == "")
        {
         // TODO: Trace error.
            throw new InvalidParameterException("Input parameter channelId is null or empty string.");
        }
        
        myChannelId = channelId;
        myMessagingSystem = messagingSystem;
    }
    
    
    public Event<ChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventApi;
    }

    public String getChannelId()
    {
        return myChannelId;
    }

    public void startListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            if (isListening())
            {
                // TODO: Trace error.
                throw new IllegalStateException("The input channel is already listening.");
            }
            
            try
            {
                myMessagingSystem.registerMessageHandler(myChannelId, myHanedleMessageImpl);
                myIsListeningFlag = true;
            }
            catch (RuntimeException err)
            {
                // TODO: Trace error.
                
                try
                {
                    stopListening();
                }
                catch (Exception err2)
                {
                }
                
                throw err;
            }
        }
        
    }

    public void stopListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            try
            {
                myMessagingSystem.unregisterMessageHandler(myChannelId);
            }
            catch (Exception err)
            {
                // TODO: Trace warning.
                //EneterTrace.Warning(TracedObject + ErrorHandler.StopListeningFailure, err);
            }

            myIsListeningFlag = false;
        }
    }

    public boolean isListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            return myIsListeningFlag;
        }
    }
    
    private void handleMessage(Object message)
    {
        if (!myMessageReceivedEventImpl.isEmpty())
        {
            try
            {
                myMessageReceivedEventImpl.update(this, new ChannelMessageEventArgs(myChannelId, message));
            }
            catch (Exception err)
            {
                // TODO: Trace warning, that the error from the handler was detected.
                //EneterTrace.Warning(TracedObject + ErrorHandler.DetectedException, err);
            }
        }
        else
        {
            // TODO: Trace warning
        }
    }
    
    private IMessagingSystemBase myMessagingSystem;
    private String myChannelId;
    private boolean myIsListeningFlag = false;
    
    private Object myListeningManipulatorLock = new Object();
    
    private EventImpl<ChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<ChannelMessageEventArgs>();
    private Event<ChannelMessageEventArgs> myMessageReceivedEventApi = new Event<ChannelMessageEventArgs>(myMessageReceivedEventImpl);
    
    private IMethod1<Object> myHanedleMessageImpl = new IMethod1<Object>()
            {
                public void invoke(Object message)
                {
                    handleMessage(message);
                }
            };

}

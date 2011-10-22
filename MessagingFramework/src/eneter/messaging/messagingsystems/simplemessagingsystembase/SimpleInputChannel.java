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
    
    
    public Event<ChannelMessageEventArgs> MessageReceived()
    {
        return myMessageReceivedEventApi;
    }

    public String GetChannelId()
    {
        return myChannelId;
    }

    public void StartListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            if (IsListening())
            {
                // TODO: Trace error.
                throw new IllegalStateException("The input channel is already listening.");
            }
            
            try
            {
                myMessagingSystem.RegisterMessageHandler(myChannelId, myHanedleMessageImpl);
                myIsListeningFlag = true;
            }
            catch (Exception err)
            {
                // TODO: Trace error.
                
                try
                {
                    StopListening();
                }
                catch (Exception err2)
                {
                }
                
                // TODO: How to rethrow?
                //throw err;
            }
        }
        
    }

    public void StopListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            try
            {
                myMessagingSystem.UnregisterMessageHandler(myChannelId);
            }
            catch (Exception err)
            {
                // TODO: Trace warning.
                //EneterTrace.Warning(TracedObject + ErrorHandler.StopListeningFailure, err);
            }

            myIsListeningFlag = false;
        }
    }

    public boolean IsListening()
    {
        synchronized (myListeningManipulatorLock)
        {
            return myIsListeningFlag;
        }
    }
    
    private void HandleMessage(Object message)
    {
        if (!myMessageReceivedEventImpl.IsEmpty())
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
                    HandleMessage(message);
                }
            };

}

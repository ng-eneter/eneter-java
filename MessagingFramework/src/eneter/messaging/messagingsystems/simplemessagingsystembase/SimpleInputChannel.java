/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase;

import java.security.InvalidParameterException;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.EProtocolMessageType;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;
import eneter.messaging.messagingsystems.messagingsystembase.ChannelMessageEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IInputChannel;
import eneter.net.system.Event;
import eneter.net.system.EventImpl;
import eneter.net.system.IMethod1;
import eneter.net.system.StringExt;

public class SimpleInputChannel implements IInputChannel
{
    public SimpleInputChannel(String channelId, IMessagingSystemBase messagingSystem, IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(channelId))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new InvalidParameterException(ErrorHandler.NullOrEmptyChannelId);
            }
            
            myChannelId = channelId;
            myMessagingSystem = messagingSystem;
            myProtocolFormatter= protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public Event<ChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventImpl.getApi();
    }

    public String getChannelId()
    {
        return myChannelId;
    }

    public void startListening()
        throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
                
                try
                {
                    myMessagingSystem.registerMessageHandler(myChannelId, myHanedleMessageImpl);
                    myIsListeningFlag = true;
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                    stopListening();
                    
                    throw err;
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                    stopListening();
                    
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                try
                {
                    myMessagingSystem.unregisterMessageHandler(myChannelId);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.StopListeningFailure, err);
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StopListeningFailure, err);
                    throw err;
                }
    
                myIsListeningFlag = false;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public boolean isListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                return myIsListeningFlag;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void handleMessage(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Decode the message.
            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(message);
            
            if (aProtocolMessage.MessageType != EProtocolMessageType.MessageReceived)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                return;
            }
            
            if (myMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myMessageReceivedEventImpl.raise(this, new ChannelMessageEventArgs(myChannelId, message));
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private IMessagingSystemBase myMessagingSystem;
    private String myChannelId;
    private boolean myIsListeningFlag = false;
    
    private Object myListeningManipulatorLock = new Object();
    
    private IProtocolFormatter<?> myProtocolFormatter;
    
    private EventImpl<ChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<ChannelMessageEventArgs>();

    
    private IMethod1<Object> myHanedleMessageImpl = new IMethod1<Object>()
            {
                public void invoke(Object message)
                {
                    handleMessage(message);
                }
            };

            
    private String TracedObject()
    {
        return "The input channel '" + myChannelId + "' ";
    }
}

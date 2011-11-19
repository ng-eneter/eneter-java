/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright � 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.infrastructure.attachable.AttachableInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.ChannelMessageEventArgs;
import eneter.net.system.*;

class TypedMessageReceiver<_MessageDataType> extends AttachableInputChannelBase
                                             implements ITypedMessageReceiver<_MessageDataType>
{
    public TypedMessageReceiver(ISerializer serializer, Class<_MessageDataType> messageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
            myMessageClazz = messageClazz;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public Event<TypedMessageReceivedEventArgs<_MessageDataType>> messageReceived()
    {
        return myMessageReceivedEventApi;
    }

    @Override
    protected void onMessageReceived(Object sender, ChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageReceivedEventImpl.isEmpty() == false)
            {
                // Deserialize the message value.
                TypedMessageReceivedEventArgs<_MessageDataType> aMessageReceivedEventArgs = null;
                try
                {
                    _MessageDataType aMessageData = mySerializer.deserialize(e.getMessage(), myMessageClazz);
                    aMessageReceivedEventArgs = new TypedMessageReceivedEventArgs<_MessageDataType>(aMessageData);
                }
                catch (Exception err)
                {
                    String aMessage = TracedObject() + "failed to deserialize the incoming message.";
                    EneterTrace.error(aMessage, err);
                    aMessageReceivedEventArgs = new TypedMessageReceivedEventArgs<_MessageDataType>(err);
                }

                try
                {
                    myMessageReceivedEventImpl.update(this, aMessageReceivedEventArgs);
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


    private Class<_MessageDataType> myMessageClazz;
    private ISerializer mySerializer;
    
    private EventImpl<TypedMessageReceivedEventArgs<_MessageDataType>> myMessageReceivedEventImpl = new EventImpl<TypedMessageReceivedEventArgs<_MessageDataType>>();
    private Event<TypedMessageReceivedEventArgs<_MessageDataType>> myMessageReceivedEventApi = new Event<TypedMessageReceivedEventArgs<_MessageDataType>>(myMessageReceivedEventImpl);
    
    private String TracedObject()
    {
        String aMessageTypeName = (myMessageClazz != null) ? myMessageClazz.getSimpleName() : "...";
        String anInputChannelId = (getAttachedInputChannel() != null) ? getAttachedInputChannel().getChannelId() : "";
        return "The TypedMessageReceiver<" + aMessageTypeName + "> atached to the duplex input channel '" + anInputChannelId + "' ";
    }
}
/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableOutputChannelBase;

class TypedMessageSender<_MessageData> extends AttachableOutputChannelBase
                                       implements ITypedMessageSender<_MessageData>
{
    public TypedMessageSender(ISerializer serializer, Class<_MessageData> messageClazz)
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
    public void sendMessage(_MessageData messageData) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (getAttachedOutputChannel() == null)
            {
                String anError = TracedObject() + "failed to send the message because it is not attached to any output channel.";
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }

            try
            {
                // Serialize the message
                Object aSerializedMessageData = mySerializer.serialize(messageData, myMessageClazz);

                // Send the message
                getAttachedOutputChannel().sendMessage(aSerializedMessageData);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private Class<_MessageData> myMessageClazz;
    
    private ISerializer mySerializer;
    
    @Override
    protected String TracedObject()
    {
        String aMessageTypeName = (myMessageClazz != null) ? myMessageClazz.getSimpleName() : "...";
        String anOutputChannelId = (getAttachedOutputChannel() != null) ? getAttachedOutputChannel().getChannelId() : "";
        return getClass().getSimpleName() + "<" + aMessageTypeName + "> atached to the duplex input channel '" + anOutputChannelId + "' ";
    }
}

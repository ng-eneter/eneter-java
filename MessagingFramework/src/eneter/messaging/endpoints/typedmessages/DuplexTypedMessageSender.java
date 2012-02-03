/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.*;
import eneter.messaging.infrastructure.attachable.*;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelMessageEventArgs;
import eneter.net.system.*;

class DuplexTypedMessageSender<_ResponseType, _RequestType> extends AttachableDuplexOutputChannelBase
                                                            implements IDuplexTypedMessageSender<_ResponseType, _RequestType>
{
    public DuplexTypedMessageSender(ISerializer serializer, Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
            myResponseMessageClazz = responseMessageClazz;
            myRequestMessageClazz = requestMessageClazz;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public Event<TypedResponseReceivedEventArgs<_ResponseType>> responseReceived()
    {
        return myResponseReceivedEventImpl.getApi();
    }

    @Override
    public void sendRequestMessage(_RequestType message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (getAttachedDuplexOutputChannel() == null)
            {
                String anError = TracedObject() + "failed to send the request message because it is not attached to any duplex output channel.";
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }

            try
            {
                Object aRequestMessage = mySerializer.serialize(message, myRequestMessageClazz);
                getAttachedDuplexOutputChannel().sendMessage(aRequestMessage);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                throw err;
            }
            catch (Error err)
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

    @Override
    protected void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (!myResponseReceivedEventImpl.isSubscribed())
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
                return;
            }

            TypedResponseReceivedEventArgs<_ResponseType> aResponseReceivedEventArgs = null;

            try
            {
                _ResponseType aResponseMessage = mySerializer.deserialize(e.getMessage(), myResponseMessageClazz);
                aResponseReceivedEventArgs = new TypedResponseReceivedEventArgs<_ResponseType>(aResponseMessage);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "failed to deserialize the response message.", err);
                aResponseReceivedEventArgs = new TypedResponseReceivedEventArgs<_ResponseType>(err);
            }

            try
            {
                myResponseReceivedEventImpl.raise(this, aResponseReceivedEventArgs);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private EventImpl<TypedResponseReceivedEventArgs<_ResponseType>> myResponseReceivedEventImpl = new EventImpl<TypedResponseReceivedEventArgs<_ResponseType>>();
    
    
    private Class<_RequestType> myRequestMessageClazz;
    private Class<_ResponseType> myResponseMessageClazz;
    
    private ISerializer mySerializer;
    
    @Override
    protected String TracedObject()
    {
        String aResponseMessageTypeName = (myResponseMessageClazz != null) ? myResponseMessageClazz.getSimpleName() : "...";
        String aRequestMessageTypeName = (myRequestMessageClazz != null) ? myRequestMessageClazz.getSimpleName() : "...";
        String aDuplexOutputChannelId = (getAttachedDuplexOutputChannel() != null) ? getAttachedDuplexOutputChannel().getChannelId() : "";
        return "The DuplexTypedMessageSender<" + aResponseMessageTypeName + ", " + aRequestMessageTypeName + "> atached to the duplex output channel '" + aDuplexOutputChannelId + "' ";
    }

}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.*;
import eneter.messaging.infrastructure.attachable.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelMessageEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.*;


class DuplexTypedMessageReceiver<_ResponseType, _RequestType> extends AttachableDuplexInputChannelBase
                                                              implements IDuplexTypedMessageReceiver<_ResponseType, _RequestType>
{
    public DuplexTypedMessageReceiver(ISerializer serializer, Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
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
    public Event<TypedRequestReceivedEventArgs<_RequestType>> messageReceived()
    {
        return myMessageReceivedEventImpl.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEventImpl.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEventImpl.getApi();
    }

    @Override
    public void sendResponseMessage(String responseReceiverId, _ResponseType responseMessage) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (getAttachedDuplexInputChannel() == null)
            {
                String anError = TracedObject() + "failed to send the response message because it is not attached to any duplex input channel.";
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }

            try
            {
                Object aResponseMessage = mySerializer.serialize(responseMessage, myResponseMessageClazz);
                getAttachedDuplexInputChannel().sendResponseMessage(responseReceiverId, aResponseMessage);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
                throw err;
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
                throw err;
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
            if (!myMessageReceivedEventImpl.isSubscribed())
            {
                EneterTrace.warning(TracedObject() + "received the request message but nobody was subscribed.");
                return;
            }

            TypedRequestReceivedEventArgs<_RequestType> aRequestReceivedEventArgs = null;

            try
            {
                _RequestType aRequestMessage = mySerializer.deserialize(e.getMessage(), myRequestMessageClazz);
                aRequestReceivedEventArgs = new TypedRequestReceivedEventArgs<_RequestType>(e.getResponseReceiverId(), aRequestMessage);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + "failed to deserialize the request message.", err);
                aRequestReceivedEventArgs = new TypedRequestReceivedEventArgs<_RequestType>(e.getResponseReceiverId(), err);
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize the request message.", err);
                throw err;
            }

            try
            {
                myMessageReceivedEventImpl.update(this, aRequestReceivedEventArgs);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverConnectedEventImpl.isSubscribed())
            {
                try
                {
                    myResponseReceiverConnectedEventImpl.update(this, e);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverDisconnectedEventImpl.isSubscribed())
            {
                try
                {
                    myResponseReceiverDisconnectedEventImpl.update(this, e);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private EventImpl<TypedRequestReceivedEventArgs<_RequestType>> myMessageReceivedEventImpl = new EventImpl<TypedRequestReceivedEventArgs<_RequestType>>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventImpl = new EventImpl<ResponseReceiverEventArgs>();
    
    
    private Class<_RequestType> myRequestMessageClazz;
    private Class<_ResponseType> myResponseMessageClazz;
    
    private ISerializer mySerializer;
    

    @Override
    protected String TracedObject()
    {
        String aResponseMessageTypeName = (myResponseMessageClazz != null) ? myResponseMessageClazz.getSimpleName() : "...";
        String aRequestMessageTypeName = (myRequestMessageClazz != null) ? myRequestMessageClazz.getSimpleName() : "...";
        String aDuplexInputChannelId = (getAttachedDuplexInputChannel() != null) ? getAttachedDuplexInputChannel().getChannelId() : "";
        return "The DuplexTypedMessageReceiver<" + aResponseMessageTypeName + ", " + aRequestMessageTypeName + "> atached to the duplex input channel '" + aDuplexInputChannelId + "' ";
    }

}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.util.UUID;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.endpoints.typedmessages.internal.ReliableMessage;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;

class ReliableDuplexTypedMessageReceiver<_ResponseType, _RequestType> implements IReliableTypedMessageReceiver<_ResponseType, _RequestType>
{
    @Override
    public Event<TypedRequestReceivedEventArgs<_RequestType>> messageReceived()
    {
        return myMessageReceivedEvent.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEvent.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEvent.getApi();
    }

    @Override
    public Event<ReliableMessageIdEventArgs> responseMessageDelivered()
    {
        return myResponseMessageDeliveredEvent.getApi();
    }

    @Override
    public Event<ReliableMessageIdEventArgs> responseMessageNotDelivered()
    {
        return myResponseMessageNotDeliveredEvent.getApi();
    }
    

    public ReliableDuplexTypedMessageReceiver(int messageAcknowledgeTimeout, ISerializer serializer,
            Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myTimeTracker = new ReliableMessageTimeTracker(messageAcknowledgeTimeout);
            myTimeTracker.trackingTimeout().subscribe(myTrackingTimeoutEventHandler);

            mySerializer = serializer;

            IDuplexTypedMessagesFactory aReceiverFactory = new DuplexTypedMessagesFactory(serializer);
            myReceiver = aReceiverFactory.createDuplexTypedMessageReceiver(ReliableMessage.class, ReliableMessage.class);
            myReceiver.messageReceived().subscribe(myRequestReceivedEventHandler);
            myReceiver.responseReceiverConnected().subscribe(myResponseReceiverConnectedEventHandler);
            myReceiver.responseReceiverDisconnected().subscribe(myResponseReceiverDisconnectedEventHandler);
            
            myRequestMessageClazz = requestMessageClazz;
            myResponseMessageClazz = responseMessageClazz;
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
            myReceiver.attachDuplexInputChannel(duplexInputChannel);
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
            myReceiver.detachDuplexInputChannel();
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
            return myReceiver.isDuplexInputChannelAttached();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexInputChannel getAttachedDuplexInputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myReceiver.getAttachedDuplexInputChannel();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    

    @Override
    public String sendResponseMessage(String responseReceiverId, _ResponseType responseMessage) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Create identifier for the reliable message.
            String aMessageId = UUID.randomUUID().toString();

            try
            {
                // Serialize the response message.
                Object aSerializedResponseMessage = mySerializer.serialize(responseMessage, myResponseMessageClazz);

                // Create reliable message.
                ReliableMessage aReliableMessage = new ReliableMessage(aMessageId, aSerializedResponseMessage);

                // Create tracking of the response message.
                // (it will check whether the receiving of the message was acknowledged in the specified timeout.)
                myTimeTracker.AddTracking(aMessageId);

                // Send the response message.
                myReceiver.sendResponseMessage(responseReceiverId, aReliableMessage);

                return aMessageId;
            }
            catch (Exception err)
            {
                // Remove the message from the tracking.
                myTimeTracker.RemoveTracking(aMessageId);

                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private void onMessageReceived(Object sender, TypedRequestReceivedEventArgs<ReliableMessage> e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // If no error during receiving the response message.
            if (e.getReceivingError() == null)
            {
                // If it is an acknowledge that a response message was delivered.
                if (e.getRequestMessage().MessageType == ReliableMessage.EMessageType.Acknowledge)
                {
                    // The acknowledge was delivered so we can remove tracking.
                    myTimeTracker.RemoveTracking(e.getRequestMessage().MessageId);

                    // Notify that response message was received.
                    notifyMessageDeliveryStatus(myResponseMessageDeliveredEvent, e.getRequestMessage().MessageId);
                }
                // If it is a request message.
                else
                {
                    // Send back the acknowledge that the request message was delivered.
                    try
                    {
                        ReliableMessage anAcknowledge = new ReliableMessage(e.getRequestMessage().MessageId);
                        myReceiver.sendResponseMessage(e.getResponseReceiverId(), anAcknowledge);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + "failed to send the acknowledge message.", err);
                    }

                    // Notify the request message was received.
                    if (myMessageReceivedEvent.isSubscribed())
                    {
                        TypedRequestReceivedEventArgs<_RequestType> aMsg = null;

                        try
                        {
                            // Deserialize the incoming request message.
                            _RequestType aRequestMessage = mySerializer.deserialize(e.getRequestMessage().Message, myRequestMessageClazz);
                            aMsg = new TypedRequestReceivedEventArgs<_RequestType>(e.getResponseReceiverId(), e.getSenderAddress(), aRequestMessage);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to deserialize the request message.", err);
                            aMsg = new TypedRequestReceivedEventArgs<_RequestType>(e.getResponseReceiverId(), e.getSenderAddress(), err);
                        }

                        try
                        {
                            myMessageReceivedEvent.raise(this, aMsg);
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
            }
            else
            {
                // Notify that an error occured during receiving the message.
                if (myMessageReceivedEvent.isSubscribed())
                {
                    TypedRequestReceivedEventArgs<_RequestType> aMsg = new TypedRequestReceivedEventArgs<_RequestType>(e.getResponseReceiverId(), e.getSenderAddress(), e.getReceivingError());
                    
                    try
                    {
                        myMessageReceivedEvent.raise(this, aMsg);
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
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    private void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notifyConnectionStatus(myResponseReceiverDisconnectedEvent, e);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notifyConnectionStatus(myResponseReceiverConnectedEvent, e);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onTrackingTimeout(Object sender, ReliableMessageIdEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // The acknowledgement was not received in the specified timeout so notify
            // that the response message was not delivered.
            notifyMessageDeliveryStatus(myResponseMessageNotDeliveredEvent, e.getMessageId());
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyConnectionStatus(EventImpl<ResponseReceiverEventArgs> handler, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler.isSubscribed())
            {
                try
                {
                    handler.raise(this, e);
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
    
    private void notifyMessageDeliveryStatus(EventImpl<ReliableMessageIdEventArgs> handler, String messageId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler.isSubscribed())
            {
                try
                {
                    ReliableMessageIdEventArgs aMsg = new ReliableMessageIdEventArgs(messageId);
                    handler.raise(this, aMsg);
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
    
    
    
    private ISerializer mySerializer;
    private IDuplexTypedMessageReceiver<ReliableMessage, ReliableMessage> myReceiver;
    private ReliableMessageTimeTracker myTimeTracker;
    
    private EventImpl<TypedRequestReceivedEventArgs<_RequestType>> myMessageReceivedEvent = new EventImpl<TypedRequestReceivedEventArgs<_RequestType>>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ReliableMessageIdEventArgs> myResponseMessageDeliveredEvent = new EventImpl<ReliableMessageIdEventArgs>();
    private EventImpl<ReliableMessageIdEventArgs> myResponseMessageNotDeliveredEvent = new EventImpl<ReliableMessageIdEventArgs>();
    
    private Class<_RequestType> myRequestMessageClazz;
    private Class<_ResponseType> myResponseMessageClazz;
    
    private EventHandler<ReliableMessageIdEventArgs> myTrackingTimeoutEventHandler = new EventHandler<ReliableMessageIdEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ReliableMessageIdEventArgs e)
        {
            onTrackingTimeout(sender, e);
        }
    };
    
    private EventHandler<TypedRequestReceivedEventArgs<ReliableMessage>> myRequestReceivedEventHandler = new EventHandler<TypedRequestReceivedEventArgs<ReliableMessage>>()
    {
        @Override
        public void onEvent(Object sender, TypedRequestReceivedEventArgs<ReliableMessage> e)
        {
            onMessageReceived(sender, e);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myResponseReceiverConnectedEventHandler = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverConnected(sender, e);
        }
    };

    private EventHandler<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEventHandler = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onResponseReceiverDisconnected(sender, e);
        }
    };
    
    private String TracedObject()
    {
        String aResponseMessageTypeName = (myResponseMessageClazz != null) ? myResponseMessageClazz.getSimpleName() : "...";
        String aRequestMessageTypeName = (myRequestMessageClazz != null) ? myRequestMessageClazz.getSimpleName() : "...";
        String aDuplexInputChannelId = (getAttachedDuplexInputChannel() != null) ? getAttachedDuplexInputChannel().getChannelId() : "";
        return "ReliableDuplexTypedMessageReceiver<" + aResponseMessageTypeName + ", " + aRequestMessageTypeName + "> atached to the duplex input channel '" + aDuplexInputChannelId + "' ";
    }
}

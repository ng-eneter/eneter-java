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
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.net.system.*;


class ReliableDuplexTypedMessageSender<_ResponseType, _RequestType> implements IReliableTypedMessageSender<_ResponseType, _RequestType>
{
    @Override
    public Event<TypedResponseReceivedEventArgs<_ResponseType>> responseReceived()
    {
        return myResponseReceivedEvent.getApi();
    }

    @Override
    public Event<ReliableMessageIdEventArgs> messageDelivered()
    {
        return myMessageDeliveredEvent.getApi();
    }

    @Override
    public Event<ReliableMessageIdEventArgs> messageNotDelivered()
    {
        return myMessageNotDeliveredEvent.getApi();
    }
    
    
    public ReliableDuplexTypedMessageSender(int messageAcknowledgeTimeout, ISerializer serializer,
            Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myTimeTracker = new ReliableMessageTimeTracker(messageAcknowledgeTimeout);
            myTimeTracker.trackingTimeout().subscribe(myTrackingTimeoutEventHandler);

            mySerializer = serializer;

            IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory(serializer);
            mySender = aSenderFactory.createDuplexTypedMessageSender(ReliableMessage.class, ReliableMessage.class);
            mySender.responseReceived().subscribe(myResponseReceivedEventHandler);
            
            myResponseMessageClazz = responseMessageClazz;
            myRequestMessageClazz = requestMessageClazz;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public void attachDuplexOutputChannel(
            IDuplexOutputChannel duplexOutputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySender.attachDuplexOutputChannel(duplexOutputChannel);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachDuplexOutputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySender.detachDuplexOutputChannel();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isDuplexOutputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return mySender.isDuplexOutputChannelAttached();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel getAttachedDuplexOutputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return mySender.getAttachedDuplexOutputChannel();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    

    @Override
    public String sendRequestMessage(_RequestType message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Create identifier for the reliable message.
            String aMessageId = UUID.randomUUID().toString();

            try
            {
                // Create tracking of the message.
                // (it will check whether the receiving of the message was acknowledged in the specified timeout.)
                myTimeTracker.AddTracking(aMessageId);

                // Create the reliable message.
                Object aSerializedRequest = mySerializer.serialize(message, myRequestMessageClazz);
                ReliableMessage aReliableMessage = new ReliableMessage(aMessageId, aSerializedRequest);

                // Send reliable message.
                mySender.sendRequestMessage(aReliableMessage);

                // Return id identifying the message.
                return aMessageId;
            }
            catch (Exception err)
            {
                // Remove the tracing.
                myTimeTracker.RemoveTracking(aMessageId);

                EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private void onResponseReceived(Object sender, TypedResponseReceivedEventArgs<ReliableMessage> e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // If no error during receiving the message.
            if (e.getReceivingError() == null)
            {
                // if it is an acknowledge that a request message was received.
                if (e.getResponseMessage().MessageType == ReliableMessage.EMessageType.Acknowledge)
                {
                    // The acknowledge was delivered so we can remove tracking.
                    myTimeTracker.RemoveTracking(e.getResponseMessage().MessageId);

                    // Notify the request message was received.
                    notifyMessageDeliveryStatus(myMessageDeliveredEvent, e.getResponseMessage().MessageId);
                }
                // If it is a response message.
                else
                {
                    // Send back the acknowledge that the response message was delivered.
                    try
                    {
                        ReliableMessage anAcknowledge = new ReliableMessage(e.getResponseMessage().MessageId);
                        mySender.sendRequestMessage(anAcknowledge);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + "failed to send the acknowledge message.", err);
                    }

                    // Notify the response message was received.
                    if (myResponseReceivedEvent.isSubscribed())
                    {
                        TypedResponseReceivedEventArgs<_ResponseType> aMsg = null;

                        try
                        {
                            _ResponseType aResponseMessage = mySerializer.deserialize(e.getResponseMessage().Message, myResponseMessageClazz);
                            aMsg = new TypedResponseReceivedEventArgs<_ResponseType>(aResponseMessage);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to deserialize the response message.", err);
                            aMsg = new TypedResponseReceivedEventArgs<_ResponseType>(err);
                        }

                        try
                        {
                            myResponseReceivedEvent.raise(this, aMsg);
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
                if (myResponseReceivedEvent.isSubscribed())
                {
                    try
                    {
                        TypedResponseReceivedEventArgs<_ResponseType> aMsg = new TypedResponseReceivedEventArgs<_ResponseType>(e.getReceivingError());
                        myResponseReceivedEvent.raise(this, aMsg);
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

    private void onTrackingTimeout(Object sender, ReliableMessageIdEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // The acknowledgement was not received in the specified timeout so notify
            // that the request message was not delivered.
            notifyMessageDeliveryStatus(myMessageNotDeliveredEvent, e.getMessageId());
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
    private IDuplexTypedMessageSender<ReliableMessage, ReliableMessage> mySender;
    private ReliableMessageTimeTracker myTimeTracker;
    
    private EventImpl<TypedResponseReceivedEventArgs<_ResponseType>> myResponseReceivedEvent = new EventImpl<TypedResponseReceivedEventArgs<_ResponseType>>();
    private EventImpl<ReliableMessageIdEventArgs> myMessageDeliveredEvent = new EventImpl<ReliableMessageIdEventArgs>();
    private EventImpl<ReliableMessageIdEventArgs> myMessageNotDeliveredEvent = new EventImpl<ReliableMessageIdEventArgs>();
    
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
    
    private EventHandler<TypedResponseReceivedEventArgs<ReliableMessage>> myResponseReceivedEventHandler = new EventHandler<TypedResponseReceivedEventArgs<ReliableMessage>>()
    {
        @Override
        public void onEvent(Object sender, TypedResponseReceivedEventArgs<ReliableMessage> e)
        {
            onResponseReceived(sender, e);
        }
    };
    
    private String TracedObject()
    {
        String aResponseMessageTypeName = (myResponseMessageClazz != null) ? myResponseMessageClazz.getSimpleName() : "...";
        String aRequestMessageTypeName = (myRequestMessageClazz != null) ? myRequestMessageClazz.getSimpleName() : "...";
        String aDuplexOutputChannelId = (getAttachedDuplexOutputChannel() != null) ? getAttachedDuplexOutputChannel().getChannelId() : "";
        return getClass().getSimpleName() + "<" + aResponseMessageTypeName + ", " + aRequestMessageTypeName + "> atached to the duplex output channel '" + aDuplexOutputChannelId + "' ";
    }
}

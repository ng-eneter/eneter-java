/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.*;

class MessageBusInputConnector implements IInputConnector
{
    public MessageBusInputConnector(ISerializer serializer, IDuplexOutputChannel messageBusOutputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myServiceId = messageBusOutputChannel.getResponseReceiverId();
            mySerializer = serializer;
            myMessageBusOutputChannel = messageBusOutputChannel;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void startListening(IMethod1<MessageContext> messageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (messageHandler == null)
            {
                throw new IllegalArgumentException("messageHandler is null.");
            }
            
            synchronized (myListeningManipulatorLock)
            {
                try
                {
                    myMessageHandler = messageHandler;
                    myMessageBusOutputChannel.responseMessageReceived().subscribe(myOnMessageFromMessageBusReceived);

                    // Open connection with the message bus.
                    myMessageBusOutputChannel.openConnection();

                    // Register service in the message bus.
                    MessageBusMessage aMessage = new MessageBusMessage(EMessageBusRequest.RegisterService, myServiceId, null);
                    Object aSerializedMessage = mySerializer.serialize(aMessage, MessageBusMessage.class);

                    myMessageBusOutputChannel.sendMessage(aSerializedMessage);
                }
                catch (Exception err)
                {
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

    @Override
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessageBusOutputChannel.closeConnection();
            myMessageBusOutputChannel.responseMessageReceived().subscribe(myOnMessageFromMessageBusReceived);
            myMessageHandler = null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening()
    {
        return myMessageBusOutputChannel.isConnected();
    }

    @Override
    public void sendResponseMessage(String clientId, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            MessageBusMessage aMessage = new MessageBusMessage(EMessageBusRequest.SendResponseMessage, clientId, message);
            Object aSerializedMessage = mySerializer.serialize(aMessage, MessageBusMessage.class);

            myMessageBusOutputChannel.sendMessage(aSerializedMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    @Override
    public void closeConnection(String clientId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            MessageBusMessage aMessage = new MessageBusMessage(EMessageBusRequest.DisconnectClient, clientId, null);
            Object aSerializedMessage = mySerializer.serialize(aMessage, MessageBusMessage.class);

            myMessageBusOutputChannel.sendMessage(aSerializedMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onMessageFromMessageBusReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            MessageBusMessage aMessageBusMessage;
            try
            {
                aMessageBusMessage = mySerializer.deserialize(e.getMessage(), MessageBusMessage.class);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize message.", err);
                return;
            }
            
            if (aMessageBusMessage.Request == EMessageBusRequest.ConnectClient)
            {
                try
                {
                    MessageBusMessage aResponseMessage = new MessageBusMessage(EMessageBusRequest.ConfirmClient, aMessageBusMessage.Id, null);
                    Object aSerializedResponse = mySerializer.serialize(aResponseMessage, MessageBusMessage.class);
                    myMessageBusOutputChannel.sendMessage(aSerializedResponse);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToReceiveMessage);
                    return;
                }

                ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.OpenConnectionRequest, aMessageBusMessage.Id, null);
                MessageContext aMessageContext = new MessageContext(aProtocolMessage, e.getSenderAddress());
                notifyMessageContext(aMessageContext);
            }
            else if (aMessageBusMessage.Request == EMessageBusRequest.DisconnectClient)
            {
                ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, aMessageBusMessage.Id, aMessageBusMessage.MessageData);
                MessageContext aMessageContext = new MessageContext(aProtocolMessage, e.getSenderAddress());
                notifyMessageContext(aMessageContext);
            }
            else if (aMessageBusMessage.Request == EMessageBusRequest.SendRequestMessage)
            {
                ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.MessageReceived, aMessageBusMessage.Id, aMessageBusMessage.MessageData);
                MessageContext aMessageContext = new MessageContext(aProtocolMessage, e.getSenderAddress());
                notifyMessageContext(aMessageContext);
            }
        }
        
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private void notifyMessageContext(MessageContext messageContext)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageHandler != null)
            {
                try
                {
                    myMessageHandler.invoke(messageContext);
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
    
    
    private String myServiceId;
    private ISerializer mySerializer;
    private IDuplexOutputChannel myMessageBusOutputChannel;
    private IMethod1<MessageContext> myMessageHandler;
    private Object myListeningManipulatorLock = new Object();
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnMessageFromMessageBusReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onMessageFromMessageBusReceived(sender, e);
        }
    };
    

    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

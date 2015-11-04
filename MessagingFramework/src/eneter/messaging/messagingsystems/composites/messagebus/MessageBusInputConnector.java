/**
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import java.util.ArrayList;
import java.util.HashMap;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.*;
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
            
            myListeningManipulatorLock.lock();
            try
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
            finally
            {
                myListeningManipulatorLock.unlock();
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
            myListeningManipulatorLock.lock();
            try
            {
                myMessageBusOutputChannel.closeConnection();
                myMessageBusOutputChannel.responseMessageReceived().unsubscribe(myOnMessageFromMessageBusReceived);
                myMessageHandler = null;
            }
            finally
            {
                myListeningManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening()
    {
        myListeningManipulatorLock.lock();
        try
        {
            return myMessageBusOutputChannel.isConnected();
        }
        finally
        {
            myListeningManipulatorLock.unlock();
        }
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
    public void sendBroadcast(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ArrayList<String> aDisconnectedClients = new ArrayList<String>();

            myConnectedClientsLock.lock();
            try
            {
                for (String aClientId : myConnectedClients.keySet())
                {
                    try
                    {
                        MessageBusMessage aMessage = new MessageBusMessage(EMessageBusRequest.SendResponseMessage, aClientId, message);
                        Object aSerializedMessage = mySerializer.serialize(aMessage, MessageBusMessage.class);
                        myMessageBusOutputChannel.sendMessage(aSerializedMessage);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + ErrorHandler.FailedToSendResponseMessage, err);
                        aDisconnectedClients.add(aClientId);

                        // Note: Exception is not rethrown because if sending to one client fails it should not
                        //       affect sending to other clients.
                    }
                }
            }
            finally
            {
                myConnectedClientsLock.unlock();
            }

            // Disconnect failed clients.
            for (String anOutputConnectorAddress : aDisconnectedClients)
            {
                closeConnection(anOutputConnectorAddress, true);
            }
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
            closeConnection(clientId, false);
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
                final MessageContext aMessageContext = new MessageContext(aProtocolMessage, e.getSenderAddress());
                
                IThreadDispatcher aDispatcher = myThreadDispatcherProvider.getDispatcher();
                myConnectedClientsLock.lock();
                try
                {
                    myConnectedClients.put(aProtocolMessage.ResponseReceiverId, aDispatcher);
                }
                finally
                {
                    myConnectedClientsLock.unlock();
                }
                aDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyMessageContext(aMessageContext);
                    }
                });
            }
            else if (aMessageBusMessage.Request == EMessageBusRequest.DisconnectClient)
            {
                ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, aMessageBusMessage.Id, aMessageBusMessage.MessageData);
                final MessageContext aMessageContext = new MessageContext(aProtocolMessage, e.getSenderAddress());
                
                IThreadDispatcher aDispatcher;
                myConnectedClientsLock.lock();
                try
                {
                    aDispatcher = myConnectedClients.get(aProtocolMessage.ResponseReceiverId);
                    if (aDispatcher != null)
                    {
                        myConnectedClients.remove(aProtocolMessage.ResponseReceiverId);
                    }
                    else
                    {
                        aDispatcher = myThreadDispatcherProvider.getDispatcher();
                    }
                }
                finally
                {
                    myConnectedClientsLock.unlock();
                }
                
                aDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyMessageContext(aMessageContext);
                    }
                });
            }
            else if (aMessageBusMessage.Request == EMessageBusRequest.SendRequestMessage)
            {
                ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.MessageReceived, aMessageBusMessage.Id, aMessageBusMessage.MessageData);
                final MessageContext aMessageContext = new MessageContext(aProtocolMessage, e.getSenderAddress());
                
                IThreadDispatcher aDispatcher;
                myConnectedClientsLock.lock();
                try
                {
                    aDispatcher = myConnectedClients.get(aProtocolMessage.ResponseReceiverId);
                    if (aDispatcher == null)
                    {
                        aDispatcher = myThreadDispatcherProvider.getDispatcher();
                        myConnectedClients.put(aProtocolMessage.ResponseReceiverId, aDispatcher);
                    }
                }
                finally
                {
                    myConnectedClientsLock.unlock();
                }
                
                aDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyMessageContext(aMessageContext);
                    }
                });
            }
        }
        
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void closeConnection(String clientId, boolean notifyFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                MessageBusMessage aMessage = new MessageBusMessage(EMessageBusRequest.DisconnectClient, clientId, null);
                Object aSerializedMessage = mySerializer.serialize(aMessage, MessageBusMessage.class);

                myMessageBusOutputChannel.sendMessage(aSerializedMessage);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToCloseConnection, err);
            }

            if (notifyFlag)
            {
                ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, clientId, null);
                MessageContext aMessageContext = new MessageContext(aProtocolMessage, "");

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
    private ThreadLock myListeningManipulatorLock = new ThreadLock();
    
    private IThreadDispatcherProvider myThreadDispatcherProvider = new SyncDispatching();
    private ThreadLock myConnectedClientsLock = new ThreadLock();
    private HashMap<String, IThreadDispatcher> myConnectedClients = new HashMap<String, IThreadDispatcher>();
    
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

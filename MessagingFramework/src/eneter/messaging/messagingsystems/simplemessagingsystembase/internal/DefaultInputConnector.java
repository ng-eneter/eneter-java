/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;


import java.util.ArrayList;
import java.util.HashSet;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.net.system.*;


class DefaultInputConnector implements IInputConnector
{
    public DefaultInputConnector(String inputConnectorAddress, IMessagingProvider messagingProvider, IProtocolFormatter protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputConnectorAddress = inputConnectorAddress;
            myMessagingProvider = messagingProvider;
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public void startListening(final IMethod1<MessageContext> messageHandler) throws Exception
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
                    myMessagingProvider.registerMessageHandler(myInputConnectorAddress, myOnRequestMessageReceived);
                    myIsListeningFlag = true;
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
            myConnectedClientsLock.lock();
            try
            {
                for (String anOutputConnectorAddress : myConnectedClients)
                {
                    try
                    {
                        closeConnection(anOutputConnectorAddress, false);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.FailedToCloseConnection, err);
                    }
                }

                myConnectedClients.clear();
            }
            finally
            {
                myConnectedClientsLock.unlock();
            }
            
            myListeningManipulatorLock.lock();
            try
            {
                myIsListeningFlag = false;
                myMessagingProvider.unregisterMessageHandler(myInputConnectorAddress);
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
            return myIsListeningFlag;
        }
        finally
        {
            myListeningManipulatorLock.unlock();
        }
    }

    @Override
    public void sendResponseMessage(String outputConnectorAddress, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                Object anEncodedMessage = myProtocolFormatter.encodeMessage(outputConnectorAddress, message);
                myMessagingProvider.sendMessage(outputConnectorAddress, anEncodedMessage);
            }
            catch (Exception err)
            {
                closeConnection(outputConnectorAddress, true);
                throw err;
            }
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
                // Send the response message to all connected clients.
                for (String aResponseReceiverId : myConnectedClients)
                {
                    try
                    {
                        // Send the response message.
                        sendResponseMessage(aResponseReceiverId, message);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + ErrorHandler.FailedToSendResponseMessage, err);
                        aDisconnectedClients.add(aResponseReceiverId);

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
    public void closeConnection(String outputConnectorAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            closeConnection(outputConnectorAddress, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onRequestMessageReceived(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(message);
            if (aProtocolMessage != null)
            {
                MessageContext aMessageContext = new MessageContext(aProtocolMessage, "");

                if (aProtocolMessage.MessageType == EProtocolMessageType.OpenConnectionRequest)
                {
                    boolean aNewConnectionAddedFlag = false;

                    // If the connection is not open yet.
                    myConnectedClientsLock.lock();
                    try
                    {
                        aNewConnectionAddedFlag = myConnectedClients.add(aProtocolMessage.ResponseReceiverId);
                    }
                    finally
                    {
                        myConnectedClientsLock.unlock();
                    }

                    if (aNewConnectionAddedFlag)
                    {
                        notifyMessageContext(aMessageContext);
                    }
                }
                else if (aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
                {
                    boolean aConnectionRemovedFlag = false;
                    myConnectedClientsLock.lock();
                    try
                    {
                        aConnectionRemovedFlag = myConnectedClients.remove(aProtocolMessage.ResponseReceiverId);
                    }
                    finally
                    {
                        myConnectedClientsLock.unlock();
                    }

                    if (aConnectionRemovedFlag)
                    {
                        notifyMessageContext(aMessageContext);
                    }
                }
                else
                {
                    notifyMessageContext(aMessageContext);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void closeConnection(String outputConnectorAddress, boolean notifyFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(outputConnectorAddress);
                myMessagingProvider.sendMessage(outputConnectorAddress, anEncodedMessage);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToCloseConnection, err);
            }

            if (notifyFlag)
            {
                ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, outputConnectorAddress, null);
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
            try
            {
                IMethod1<MessageContext> aMessageHandler = myMessageHandler;
                if (aMessageHandler != null)
                {
                    aMessageHandler.invoke(messageContext);
                }
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
    
    
    private String myInputConnectorAddress;
    private IMessagingProvider myMessagingProvider;
    private IProtocolFormatter myProtocolFormatter;
    private boolean myIsListeningFlag;
    private IMethod1<MessageContext> myMessageHandler;
    private ThreadLock myListeningManipulatorLock = new ThreadLock();
    private ThreadLock myConnectedClientsLock = new ThreadLock();
    private HashSet<String> myConnectedClients = new HashSet<String>();
    
    
    private IMethod1<Object> myOnRequestMessageReceived = new IMethod1<Object>()
    {
        @Override
        public void invoke(Object t) throws Exception
        {
            onRequestMessageReceived(t);
        }
    };
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

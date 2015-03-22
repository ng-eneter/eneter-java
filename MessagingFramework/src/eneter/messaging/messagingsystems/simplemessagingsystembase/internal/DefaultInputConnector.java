/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;


import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
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
            
            synchronized (myListeningManipulatorLock)
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
            synchronized (myListeningManipulatorLock)
            {
                myIsListeningFlag = false;
                myMessagingProvider.unregisterMessageHandler(myInputConnectorAddress);
                myMessageHandler = null;
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
        synchronized (myListeningManipulatorLock)
        {
            return myIsListeningFlag;
        }
    }

    @Override
    public void sendResponseMessage(String outputConnectorAddress, Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object anEncodedMessage = myProtocolFormatter.encodeMessage(outputConnectorAddress, message);
            myMessagingProvider.sendMessage(outputConnectorAddress, anEncodedMessage);
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
            Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(outputConnectorAddress);
            myMessagingProvider.sendMessage(outputConnectorAddress, anEncodedMessage);
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

                try
                {
                    if (myMessageHandler != null)
                    {
                        myMessageHandler.invoke(aMessageContext);
                    }
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
    
    
    private String myInputConnectorAddress;
    private IMessagingProvider myMessagingProvider;
    private IProtocolFormatter myProtocolFormatter;
    private Object myListeningManipulatorLock = new Object();
    private boolean myIsListeningFlag;
    private IMethod1<MessageContext> myMessageHandler;
    
    
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

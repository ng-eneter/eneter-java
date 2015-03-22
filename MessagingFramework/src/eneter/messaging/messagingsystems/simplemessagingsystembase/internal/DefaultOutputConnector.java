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

class DefaultOutputConnector implements IOutputConnector
{

    public DefaultOutputConnector(String inputConnectorAddress, String outputConnectorAddress, IMessagingProvider messagingProvider, IProtocolFormatter protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputConnectorAddress = inputConnectorAddress;
            myOutputConnectorAddress = outputConnectorAddress;
            myMessagingProvider = messagingProvider;
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    

    @Override
    public void openConnection(final IMethod1<MessageContext> responseMessageHandler) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (responseMessageHandler == null)
            {
                throw new IllegalArgumentException("responseMessageHandler is null.");
            }
            
            synchronized (myConnectionManipulatorLock)
            {
                try
                {
                    myResponseMessageHandler = responseMessageHandler;
                    myMessagingProvider.registerMessageHandler(myOutputConnectorAddress, myHandleResponseMessage);
                    
                    myIsResponseListenerRegistered = true;
                    
                    // Send the open connection request.
                    Object anEncodedMessage = myProtocolFormatter.encodeOpenConnectionMessage(myOutputConnectorAddress);
                    myMessagingProvider.sendMessage(myInputConnectorAddress, anEncodedMessage);
                    
                    myIsConnected = true;
                }
                catch (Exception err)
                {
                    closeConnection();
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
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            cleanConnection(true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        synchronized (myConnectionManipulatorLock)
        {
            return myIsConnected;
        }
    }
    
    @Override
    public void sendRequestMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                Object anEncodedMessage = myProtocolFormatter.encodeMessage(myOutputConnectorAddress, message);
                myMessagingProvider.sendMessage(myInputConnectorAddress, anEncodedMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void handleResponseMessage(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IMethod1<MessageContext> aResponseHandler = myResponseMessageHandler;

            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(message);
            MessageContext aMessageContext = new MessageContext(aProtocolMessage, "");

            if (aProtocolMessage != null && aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                cleanConnection(false);
            }

            try
            {
                aResponseHandler.invoke(aMessageContext);
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
    
    private void cleanConnection(boolean sendMessageFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (myIsConnected)
                {
                    if (sendMessageFlag)
                    {
                        // Send close connection message.
                        try
                        {
                            Object anEncodedMessage = myProtocolFormatter.encodeCloseConnectionMessage(myOutputConnectorAddress);
                            myMessagingProvider.sendMessage(myInputConnectorAddress, anEncodedMessage);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to send close connection message.", err);
                        }
                    }

                    myIsConnected = false;
                }

                if (myIsResponseListenerRegistered)
                {
                    myMessagingProvider.unregisterMessageHandler(myOutputConnectorAddress);
                    myResponseMessageHandler = null;
                    myIsResponseListenerRegistered = false;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    
    
    private String myInputConnectorAddress;
    private String myOutputConnectorAddress;
    private IMessagingProvider myMessagingProvider;
    private IProtocolFormatter myProtocolFormatter;
    private boolean myIsConnected;
    private boolean myIsResponseListenerRegistered;
    private Object myConnectionManipulatorLock = new Object();
    private IMethod1<MessageContext> myResponseMessageHandler;


    private IMethod1<Object> myHandleResponseMessage = new IMethod1<Object>()
    {
        @Override
        public void invoke(Object t) throws Exception
        {
            handleResponseMessage(t);
        }
    };
    
   
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

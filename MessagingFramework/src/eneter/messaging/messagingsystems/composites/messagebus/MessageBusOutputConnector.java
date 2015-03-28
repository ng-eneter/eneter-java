/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import java.util.concurrent.TimeoutException;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.*;
import eneter.net.system.threading.internal.ManualResetEvent;

class MessageBusOutputConnector implements IOutputConnector
{

    
    public MessageBusOutputConnector(String inputConnectorAddress, ISerializer serializer, IDuplexOutputChannel messageBusOutputChannel,
            int openConnectionTimeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myServiceId = inputConnectorAddress;
            myClientId = messageBusOutputChannel.getResponseReceiverId();
            mySerializer = serializer;
            myMessageBusOutputChannel = messageBusOutputChannel;
            myOpenConnectionTimeout = (openConnectionTimeout == -1) ? 0 : openConnectionTimeout;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void openConnection(IMethod1<MessageContext> responseMessageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (responseMessageHandler == null)
            {
                throw new IllegalArgumentException("responseMessageHandler is null.");
            }
            
            synchronized (myConnectionManipulator)
            {
                try
                {
                    myResponseMessageHandler = responseMessageHandler;
                    myMessageBusOutputChannel.responseMessageReceived().subscribe(myOnMessageFromMessageBusReceived);
                    myMessageBusOutputChannel.connectionClosed().subscribe(myOnConnectionWithMessageBusClosed);
                    myMessageBusOutputChannel.openConnection();

                    myOpenConnectionConfirmed.reset();
                    
                    // Tell message bus which service shall be associated with this connection.
                    MessageBusMessage aMessage = new MessageBusMessage(EMessageBusRequest.ConnectClient, myServiceId, null);
                    Object aSerializedMessage = mySerializer.serialize(aMessage, MessageBusMessage.class);
                    myMessageBusOutputChannel.sendMessage(aSerializedMessage);

                    if (!myOpenConnectionConfirmed.waitOne(myOpenConnectionTimeout))
                    {
                        throw new TimeoutException(TracedObject() + "failed to open the connection within the timeout: " + myOpenConnectionTimeout);
                    }
                    
                    if (!myMessageBusOutputChannel.isConnected())
                    {
                        throw new IllegalStateException(TracedObject() + ErrorHandler.FailedToOpenConnection);
                    }
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
            synchronized (myConnectionManipulator)
            {
                myResponseMessageHandler = null;
                
                // Close connection with the message bus.
                myMessageBusOutputChannel.closeConnection();
                myMessageBusOutputChannel.responseMessageReceived().subscribe(myOnMessageFromMessageBusReceived);
                myMessageBusOutputChannel.connectionClosed().subscribe(myOnConnectionWithMessageBusClosed);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        synchronized (myConnectionManipulator)
        {
            return myMessageBusOutputChannel.isConnected();
        }
    }

    @Override
    public void sendRequestMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: do not send the client id. It will be automatically assign in the message bus before forwarding the message to the service.
            //       It is done like this due to security reasons. So that some client cannot pretend other client just by sending a different id.
            MessageBusMessage aMessage = new MessageBusMessage(EMessageBusRequest.SendRequestMessage, "", message);
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
            
            if (aMessageBusMessage.Request == EMessageBusRequest.ConfirmClient)
            {
                // Indicate the connection is open and relase the waiting in the OpenConnection().
                myOpenConnectionConfirmed.set();

                EneterTrace.debug("CONNECTION CONFIRMED");
            }
            else if (aMessageBusMessage.Request == EMessageBusRequest.SendResponseMessage)
            {
                IMethod1<MessageContext> aResponseHandler = myResponseMessageHandler;

                if (aResponseHandler != null)
                {
                    ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.MessageReceived, myServiceId, aMessageBusMessage.MessageData);
                    MessageContext aMessageContext = new MessageContext(aProtocolMessage, e.getSenderAddress());

                    try
                    {
                        aResponseHandler.invoke(aMessageContext);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onConnectionWithMessageBusClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // In case the OpenConnection() is waiting until the connection is open relase it.
            myOpenConnectionConfirmed.set();

            IMethod1<MessageContext> aResponseHandler = myResponseMessageHandler;
            closeConnection();

            if (aResponseHandler != null)
            {
                try
                {
                    aResponseHandler.invoke(null);
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
    
    
    private String myClientId;
    private int myOpenConnectionTimeout;
    private ISerializer mySerializer;
    private IDuplexOutputChannel myMessageBusOutputChannel;
    private String myServiceId;
    private IMethod1<MessageContext> myResponseMessageHandler;
    private Object myConnectionManipulator = new Object();
    private ManualResetEvent myOpenConnectionConfirmed = new ManualResetEvent(false);
    
    private EventHandler<DuplexChannelMessageEventArgs> myOnMessageFromMessageBusReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onMessageFromMessageBusReceived(sender, e);
        }
    };
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionWithMessageBusClosed = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionWithMessageBusClosed(sender, e);
        }
    };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

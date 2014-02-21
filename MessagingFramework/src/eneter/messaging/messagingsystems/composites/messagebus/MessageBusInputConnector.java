/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import java.io.OutputStream;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.*;

class MessageBusInputConnector implements IInputConnector
{
    private class ResponseSender implements ISender
    {
        public ResponseSender(IDuplexOutputChannel messageBusOutputChannel)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myMessageBusOutputChannel = messageBusOutputChannel;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public boolean isStreamWritter()
        {
            return false;
        }

        @Override
        public void sendMessage(Object message) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myMessageBusOutputChannel.sendMessage(message);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public void sendMessage(IMethod1<OutputStream> toStreamWritter)
                throws Exception
        {
            throw new IllegalStateException();
        }
    
        private IDuplexOutputChannel myMessageBusOutputChannel;
    }
    

    
    public MessageBusInputConnector(IDuplexOutputChannel messageBusOutputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessageBusOutputChannel = messageBusOutputChannel;
            myResponseSender = new ResponseSender(myMessageBusOutputChannel);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void startListening(IFunction1<Boolean, MessageContext> messageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessageHandler = messageHandler;
            myMessageBusOutputChannel.responseMessageReceived().subscribe(myOnMessageFromMessageBusReceived);
            myMessageBusOutputChannel.connectionClosed().subscribe(myOnConnectionWithMessageBusClosed);

            // Open the connection with the service.
            // Note: the response receiver id of this output channel represents the service id inside the message bus.
            myMessageBusOutputChannel.openConnection();
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
            if (myMessageBusOutputChannel != null)
            {
                myMessageBusOutputChannel.closeConnection();
                myMessageBusOutputChannel.responseMessageReceived().subscribe(myOnMessageFromMessageBusReceived);
                myMessageBusOutputChannel.connectionClosed().subscribe(myOnConnectionWithMessageBusClosed);
            }

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
    public ISender createResponseSender(String clientId)
            throws Exception
    {
        throw new UnsupportedOperationException("CreateResponseSender is not supported.");
    }

    
    private void onMessageFromMessageBusReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageHandler != null)
            {
                try
                {
                    MessageContext aMessageContext = new MessageContext(e.getMessage(), e.getSenderAddress(), myResponseSender);
                    myMessageHandler.invoke(aMessageContext);
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
    
    private void onConnectionWithMessageBusClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageHandler != null)
            {
                try
                {
                    myMessageHandler.invoke(null);
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
    
    
    private ISender myResponseSender;
    private IDuplexOutputChannel myMessageBusOutputChannel;
    private IFunction1<Boolean, MessageContext> myMessageHandler;
    
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

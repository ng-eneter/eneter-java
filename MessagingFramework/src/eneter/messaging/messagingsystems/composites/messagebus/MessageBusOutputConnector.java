/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.*;
import eneter.net.system.threading.internal.ManualResetEvent;

class MessageBusOutputConnector implements IOutputConnector
{

    
    public MessageBusOutputConnector(String serviceAddressInMessageBus, IDuplexOutputChannel messageBusOutputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myServiceAddressInMessageBus = serviceAddressInMessageBus;
            myMessageBusOutputChannel = messageBusOutputChannel;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public void openConnection(IFunction1<Boolean, MessageContext> responseMessageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulator)
            {
                try
                {
                    myResponseMessageHandler = responseMessageHandler;
                    myMessageBusOutputChannel.responseMessageReceived().subscribe(myOnMessageFromMessageBusReceived);
                    myMessageBusOutputChannel.connectionClosed().subscribe(myOnConnectionWithMessageBusClosed);
                    myMessageBusOutputChannel.openConnection();

                    // Inform the message bus which service this client wants to connect.
                    myOpenConnectionConfirmed.reset();
                    myMessageBusOutputChannel.sendMessage(myServiceAddressInMessageBus);

                    if (!myOpenConnectionConfirmed.waitOne(30000))
                    {
                        throw new TimeoutException(TracedObject() + "failed to open the connection within the timeout.");
                    }
                }
                catch (Exception err)
                {
                    closeConnection();
                    throw err;
                }

                if (!myMessageBusOutputChannel.isConnected())
                {
                    throw new IllegalStateException(TracedObject() + ErrorHandler.OpenConnectionFailure);
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
                
                if (myMessageBusOutputChannel != null)
                {
                    myMessageBusOutputChannel.closeConnection();
                    myMessageBusOutputChannel.responseMessageReceived().subscribe(myOnMessageFromMessageBusReceived);
                    myMessageBusOutputChannel.connectionClosed().subscribe(myOnConnectionWithMessageBusClosed);
                }
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
        throw new UnsupportedOperationException();
    }
    
    private void onMessageFromMessageBusReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object aMessage = e.getMessage();
            
            // If it is a confirmation message that the connection was really open.
            if (aMessage instanceof String && ((String)aMessage).equals("OK"))
            {
                // Indicate the connection is open and relase the waiting in the OpenConnection().
                myOpenConnectionConfirmed.set();
            }
            else if (myResponseMessageHandler != null)
            {
                try
                {
                    MessageContext aMessageContext = new MessageContext(aMessage, e.getSenderAddress(), null);
                    myResponseMessageHandler.invoke(aMessageContext);
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
            // In case the OpenConnection() is waiting until the connection is open relase it.
            myOpenConnectionConfirmed.set();

            if (myResponseMessageHandler != null)
            {
                try
                {
                    myResponseMessageHandler.invoke(null);
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
    
    private IDuplexOutputChannel myMessageBusOutputChannel;
    private String myServiceAddressInMessageBus;
    private IFunction1<Boolean, MessageContext> myResponseMessageHandler;
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

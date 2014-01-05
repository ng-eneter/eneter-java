/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.messagebus;

import java.io.OutputStream;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.nodes.broker.*;
import eneter.net.system.*;


class MessageBusOutputConnector implements IOutputConnector
{
    public MessageBusOutputConnector(String inputConnectorAddresss, String outputConnectorAddress, IDuplexBrokerClient brokerClient, IDuplexOutputChannel brokerOutputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputConnectorAddress = inputConnectorAddresss;
            myOutputConnectorAddress = outputConnectorAddress;
            myBrokerClient = brokerClient;
            myBrokerClientOutputChannel = brokerOutputChannel;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public void openConnection(
            IFunction1<Boolean, MessageContext> responseMessageHandler)
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
                    myBrokerClient.brokerMessageReceived().subscribe(myOnBrokerMessageReceived);
                    myBrokerClient.attachDuplexOutputChannel(myBrokerClientOutputChannel);

                    // Subscribe to get messages from the service.
                    myBrokerClient.subscribe(myOutputConnectorAddress);
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
                try
                {
                    myBrokerClient.unsubscribe();
                }
                catch (Exception err)
                {
                }

                // Unsubscribe from all notifications.
                myBrokerClient.detachDuplexOutputChannel();
                myBrokerClient.brokerMessageReceived().unsubscribe(myOnBrokerMessageReceived);

                myResponseMessageHandler = null;
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
            return myBrokerClient.isDuplexOutputChannelAttached() && myBrokerClientOutputChannel.isConnected();
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
            myBrokerClient.sendMessage(myInputConnectorAddress, message);
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
    
    
    private void onBrokerMessageReceived(Object sender, BrokerMessageReceivedEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (e.getReceivingError() != null)
            {
                EneterTrace.warning(TracedObject() + "detected error when receiving the message from the message bus.", e.getReceivingError());
                return;
            }

            MessageContext aMessageContext = new MessageContext(e.getMessage(), "", null);
            if (myResponseMessageHandler != null)
            {
                try
                {
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
    
    
    
    private IDuplexBrokerClient myBrokerClient;
    private IDuplexOutputChannel myBrokerClientOutputChannel;
    private String myInputConnectorAddress;
    private String myOutputConnectorAddress;
    private IFunction1<Boolean, MessageContext> myResponseMessageHandler;
    private Object myConnectionManipulator = new Object();

    
    private EventHandler<BrokerMessageReceivedEventArgs> myOnBrokerMessageReceived = new EventHandler<BrokerMessageReceivedEventArgs>()
    {
        @Override
        public void onEvent(Object sender, BrokerMessageReceivedEventArgs e)
        {
            onBrokerMessageReceived(sender, e);
        }
    };
    
    private String TracedObject() { return getClass().getSimpleName() + " "; }
}

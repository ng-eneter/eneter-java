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


class MessageBusInputConnector implements IInputConnector
{
    private class ResponseSender implements ISender
    {
        public ResponseSender(String responseReceiverId, IDuplexBrokerClient brokerClient)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myResponseReceiverId = responseReceiverId;
                myBrokerClient = brokerClient;
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
                myBrokerClient.sendMessage(myResponseReceiverId, message);
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
        
        
        private String myResponseReceiverId;
        private IDuplexBrokerClient myBrokerClient;
    }
    
    
    
    public MessageBusInputConnector(String inputConnectorAddress, IDuplexBrokerClient brokerClient, IDuplexOutputChannel brokerOutputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputConnectorAddress = inputConnectorAddress;
            myBrokerClient = brokerClient;
            myBrokerClientOutputChannel = brokerOutputChannel;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void startListening(
            IFunction1<Boolean, MessageContext> messageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myMessageHandler = messageHandler;
                myBrokerClient.brokerMessageReceived().subscribe(myOnBrokerMessageReceived);
                myBrokerClient.attachDuplexOutputChannel(myBrokerClientOutputChannel);
                myBrokerClient.subscribe(myInputConnectorAddress);
            }
            catch (Exception err)
            {
                stopListening();
                throw err;
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
            if (myBrokerClient != null)
            {
                try
                {
                    myBrokerClient.unsubscribe();
                }
                catch(Exception err)
                {
                }

                myBrokerClient.detachDuplexOutputChannel();
                myBrokerClient.brokerMessageReceived().unsubscribe(myOnBrokerMessageReceived);
                myMessageHandler = null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening() throws Exception
    {
        return myBrokerClient.isDuplexOutputChannelAttached() && myBrokerClientOutputChannel.isConnected();
    }

    @Override
    public ISender createResponseSender(String responseReceiverAddress)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ResponseSender aResponseSender = new ResponseSender(responseReceiverAddress, myBrokerClient);
            return aResponseSender;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
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
            if (myMessageHandler != null)
            {
                try
                {
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
    
    private String myInputConnectorAddress;
    private IDuplexBrokerClient myBrokerClient;
    private IDuplexOutputChannel myBrokerClientOutputChannel;
    private IFunction1<Boolean, MessageContext> myMessageHandler;
    
    
    EventHandler<BrokerMessageReceivedEventArgs> myOnBrokerMessageReceived = new EventHandler<BrokerMessageReceivedEventArgs>()
    {
        @Override
        public void onEvent(Object sender, BrokerMessageReceivedEventArgs e)
        {
            onBrokerMessageReceived(sender, e);
        }
    };
    

    private String TracedObject() { return getClass().getSimpleName() + " "; }
}

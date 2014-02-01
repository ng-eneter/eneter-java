/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.messagebus;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.nodes.broker.*;
import eneter.messaging.threading.dispatching.*;


public class MessageBusMessagingFactory implements IMessagingSystemFactory
{
    private class MessageBusConnectorFactory implements IOutputConnectorFactory, IInputConnectorFactory
    {
        public MessageBusConnectorFactory(String brokerAddress, IDuplexBrokerFactory brokerFactory, IMessagingSystemFactory brokerMessaging)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myBrokerAddress = brokerAddress;
                myBrokerFactory = brokerFactory;
                myUnderlyingBrokerMessaging = brokerMessaging;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        

        @Override
        public IOutputConnector createOutputConnector(
                String serviceConnectorAddress, String clientConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                IDuplexBrokerClient aBrokerClient = myBrokerFactory.createBrokerClient();
                IDuplexOutputChannel aBrokerOutputChannel = myUnderlyingBrokerMessaging.createDuplexOutputChannel(myBrokerAddress, clientConnectorAddress);
                return new MessageBusOutputConnector(serviceConnectorAddress, clientConnectorAddress, aBrokerClient, aBrokerOutputChannel);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        
        @Override
        public IInputConnector createInputConnector(String receiverAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                IDuplexBrokerClient aBrokerClient = myBrokerFactory.createBrokerClient();
                IDuplexOutputChannel aBrokerOutputChannel = myUnderlyingBrokerMessaging.createDuplexOutputChannel(myBrokerAddress);
                return new MessageBusInputConnector(receiverAddress, aBrokerClient, aBrokerOutputChannel);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        
        private String myBrokerAddress;
        private IDuplexBrokerFactory myBrokerFactory;
        private IMessagingSystemFactory myUnderlyingBrokerMessaging;
    }
    

    public MessageBusMessagingFactory(String brokerAddress, IMessagingSystemFactory brokerMessaging)
    {
        this(brokerAddress, brokerMessaging, new XmlStringSerializer(), new EneterProtocolFormatter());
    }
    
    public MessageBusMessagingFactory(String brokerAddress, IMessagingSystemFactory brokerMessaging, ISerializer serializer)
    {
        this(brokerAddress, brokerMessaging, serializer, new EneterProtocolFormatter());
    }
    
    public MessageBusMessagingFactory(String brokerAddress, IMessagingSystemFactory brokerMessaging, ISerializer serializer, IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myProtocolFormatter = protocolFormatter;
            IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory(serializer);
            myConnectorFactory = new MessageBusConnectorFactory(brokerAddress, aBrokerFactory, brokerMessaging);

            // Dispatch events in the same thread as notified from the underlying messaging.
            myDispatcher = new NoDispatching().getDispatcher();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultDuplexOutputChannel(channelId, null, myDispatcher, myConnectorFactory, myProtocolFormatter, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, myDispatcher, myConnectorFactory, myProtocolFormatter, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IInputConnector anInputConnector = myConnectorFactory.createInputConnector(channelId);
            return new DefaultDuplexInputChannel(channelId, myDispatcher, anInputConnector, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IProtocolFormatter<?> getProtocolFormatter()
    {
        return myProtocolFormatter;
    }
    
    private IThreadDispatcher myDispatcher;
    private MessageBusConnectorFactory myConnectorFactory;
    private IProtocolFormatter<?> myProtocolFormatter;
}

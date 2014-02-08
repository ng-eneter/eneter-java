/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.*;


public class MessageBusMessagingFactory implements IMessagingSystemFactory
{
    private class MessageBusConnectorFactory implements IOutputConnectorFactory, IInputConnectorFactory
    {
        public MessageBusConnectorFactory(String serviceConnctingAddress, String clientConnectingAddress, IMessagingSystemFactory messageBusMessaging)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientConnectingAddress = clientConnectingAddress;
                myServiceConnectingAddress = serviceConnctingAddress;
                myMessageBusMessaging = messageBusMessaging;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        

        @Override
        public IOutputConnector createOutputConnector(String serviceConnectorAddress, String clientConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                IDuplexOutputChannel aMessageBusOutputChannel = myMessageBusMessaging.createDuplexOutputChannel(myClientConnectingAddress, clientConnectorAddress);
                return new MessageBusOutputConnector(serviceConnectorAddress, aMessageBusOutputChannel);
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
                // Note: message bus service address is encoded in OpenConnectionMessage when the service connects the message bus.
                //       Therefore receiverAddress (which is message bus service address) is used when creating output channel.
                IDuplexOutputChannel aMessageBusOutputChannel = myMessageBusMessaging.createDuplexOutputChannel(myServiceConnectingAddress, receiverAddress);
                return new MessageBusInputConnector(aMessageBusOutputChannel);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        private String myClientConnectingAddress;
        private String myServiceConnectingAddress;
        private IMessagingSystemFactory myMessageBusMessaging;
    }
    
    
    public MessageBusMessagingFactory(String serviceConnctingAddress, String clientConnectingAddress, IMessagingSystemFactory underlyingMessaging)
    {
        this(serviceConnctingAddress, clientConnectingAddress, underlyingMessaging, new EneterProtocolFormatter());
    }
    
    public MessageBusMessagingFactory(String serviceConnctingAddress, String clientConnectingAddress, IMessagingSystemFactory underlyingMessaging, IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectorFactory = new MessageBusConnectorFactory(serviceConnctingAddress, clientConnectingAddress, underlyingMessaging);

            // Dispatch events in the same thread as notified from the underlying messaging.
            myDispatcher = new NoDispatching().getDispatcher();

            myProtocolFormatter = protocolFormatter;
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
            DefaultDuplexInputChannel anInputChannel = new DefaultDuplexInputChannel(channelId, myDispatcher, anInputConnector, myProtocolFormatter);
            anInputChannel.includeResponseReceiverIdToResponses(true);
            return anInputChannel;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IProtocolFormatter<?> myProtocolFormatter;

    private IThreadDispatcher myDispatcher;
    private MessageBusConnectorFactory myConnectorFactory;
}

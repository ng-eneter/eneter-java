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

/**
 * Messaging system allowing the communication via the message bus.
 * 
 * The following example shows how to communicate via the message bus.<br/>
 * <br/>
 * Implementation of the message bus service that will mediate the client-service communication: 
 * <pre>
 * public class Program
 * {
 *     public static void main(String[] args) throws Exception
 *     {
 *         // Message Bus will use TCP for the communication.
 *         IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 *  <br/>
 *         // Input channel to listen to services.
 *         IDuplexInputChannel aServiceInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8045/");
 *  <br/>
 *         // Input channel to listen to clients.
 *         IDuplexInputChannel aClientInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8046/");
 *  <br/>
 *         // Create the message bus.
 *         IMessageBus aMessageBus = new MessageBusFactory().createMessageBus();
 *  <br/>
 *         // Attach channels to the message bus and start listening.
 *         aMessageBus.attachDuplexInputChannels(aServiceInputChannel, aClientInputChannel);
 *  <br/>
 *         System.out.println("Message bus service is running. Press ENTER to stop.");
 *         new BufferedReader(new InputStreamReader(System.in)).readLine();
 *  <br/>
 *         // Detach channels and stop listening.
 *         aMessageBus.detachDuplexInputChannels();
 *     }
 * }
 * </pre>
 * <br/>
 * Implementation of the service which is exposed via the message bus:
 * <pre>
 * public interface IEcho
 * {
 *     String hello(String text);
 * }<br/>
 * <br/>
 * ....
 * <br/>
 * // Simple echo service.
 * class EchoService implements IEcho
 * {
 *     {@literal @}Override
 *     public String hello(String text)
 *     {
 *         return text;
 *     }
 *  <br/>
 * }<br/>
 * <br/>
 * ....
 * <br/>
 * public class Program
 * {
 *     public static void main(String[] args) throws Exception
 *     {
 *         // The service will communicate via Message Bus which is listening via TCP.
 *         IMessagingSystemFactory aMessageBusUnderlyingMessaging = new TcpMessagingSystemFactory();
 *         // note: only TCP/IP address which is exposed for services is needed.
 *         IMessagingSystemFactory aMessaging = new MessageBusMessagingFactory("tcp://127.0.0.1:8045/", null, aMessageBusUnderlyingMessaging);
 *  <br/>
 *         // Create input channel listening via the message bus.
 *         // Note: this is address of the service inside the message bus.
 *         IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("Eneter.Echo");
 *  <br/>
 *         // Instantiate class implementing the service.
 *         IEcho anEcho = new EchoService();
 *  <br/>
 *         // Create the RPC service.
 *         IRpcService&lt;IEcho&gt; anEchoService = new RpcFactory().createService(anEcho, IEcho.class);
 *  <br/>
 *         // Attach input channel to the service and start listening via the message bus.
 *         anEchoService.attachDuplexInputChannel(anInputChannel);
 *  <br/>
 *         System.out.println("Echo service is running. Press ENTER to stop.");
 *         new BufferedReader(new InputStreamReader(System.in)).readLine();
 *  <br/>
 *         // Detach the input channel and stop listening.
 *         anEchoService.detachDuplexInputChannel();
 *     }
 * }
 * </pre>
 * <br/>
 * Implementation of the client using the service which is exposed via the message bus:
 * <pre>
 * public class Program
 * {
 *     public static void main(String[] args) throws Exception
 *     {
 *         // The client will communicate via Message Bus which is listening via TCP.
 *         IMessagingSystemFactory aMessageBusUnderlyingMessaging = new TcpMessagingSystemFactory();
 *         // note: only TCP/IP address which is exposed for clients is needed. 
 *         IMessagingSystemFactory aMessaging = new MessageBusMessagingFactory(null, "tcp://127.0.0.1:8046/", aMessageBusUnderlyingMessaging);
 *  <br/>
 *         // Create output channel that will connect the service via the message bus..
 *         // Note: this is address of the service inside the message bus.
 *         IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("Eneter.Echo");
 *  <br/>
 *         // Create the RPC client for the Echo Service.
 *         IRpcClient&lt;IEcho&gt; aClient = new RpcFactory().createClient(IEcho.class);
 *  <br/>
 *         // Attach the output channel and be able to communicate with the service via the message bus.
 *         aClient.attachDuplexOutputChannel(anOutputChannel);
 *  <br/>
 *         // Get the service proxy and call the echo method.
 *         IEcho aProxy = aClient.getProxy();
 *         String aResponse = aProxy.hello("hello");
 *  <br/>
 *         System.out.println("Echo service returned: " + aResponse);
 *  <br/>
 *         // Detach the output channel.
 *         aClient.detachDuplexOutputChannel();
 *     }
 * }
 * </pre>
 * 
 */
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
    
   
    /**
     * Constructs the factory.
     * 
     * @param serviceConnctingAddress message bus address for registered services.
     * @param clientConnectingAddress message bus address for clients that want to connect a registered service.
     * @param underlyingMessaging messaging system used by the message bus.
     */
    public MessageBusMessagingFactory(String serviceConnctingAddress, String clientConnectingAddress, IMessagingSystemFactory underlyingMessaging)
    {
        this(serviceConnctingAddress, clientConnectingAddress, underlyingMessaging, new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the factory.
     * 
     * @param serviceConnctingAddress message bus address for registered services.
     * @param clientConnectingAddress message bus address for clients that want to connect a registered service.
     * @param underlyingMessaging messaging system used by the message bus.
     * @param protocolFormatter protocol formatter used for the communication between channels.
     */
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

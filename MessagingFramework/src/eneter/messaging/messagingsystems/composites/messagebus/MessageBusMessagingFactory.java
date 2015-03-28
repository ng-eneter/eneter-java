/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.threading.dispatching.*;

/**
 * Extension providing the communication via the message bus.
 * 
 * This messaging wraps the communication with the message bus.
 * The duplex input channel created by this messaging will automatically connect the message bus and register the service
 * when the startListening() is called.<br/>
 * The duplex output channel created by this messaging will automatically connect the message bus and ask for the service
 * when the openConnection() is called.
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
                myOpenConnectionTimeout = 30000;
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
                return new MessageBusOutputConnector(serviceConnectorAddress, mySerializer, aMessageBusOutputChannel, myOpenConnectionTimeout);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        @Override
        public IInputConnector createInputConnector(String inputConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                // Note: message bus service address is encoded in OpenConnectionMessage when the service connects the message bus.
                //       Therefore receiverAddress (which is message bus service address) is used when creating output channel.
                IDuplexOutputChannel aMessageBusOutputChannel = myMessageBusMessaging.createDuplexOutputChannel(myServiceConnectingAddress, inputConnectorAddress);
                return new MessageBusInputConnector(mySerializer, aMessageBusOutputChannel);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        public int getOpenConnectionTimeout()
        {
            return myOpenConnectionTimeout;
        }

        public void setOpenConnectionTimeout(int openConnectionTimeout)
        {
            myOpenConnectionTimeout = openConnectionTimeout;
        }

        private String myClientConnectingAddress;
        private String myServiceConnectingAddress;
        private ISerializer mySerializer;
        private IMessagingSystemFactory myMessageBusMessaging;
        
        private int myOpenConnectionTimeout;
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
        this(serviceConnctingAddress, clientConnectingAddress, underlyingMessaging, new MessageBusCustomSerializer());
    }
    
    /**
     * Constructs the factory.
     * 
     * @param serviceConnctingAddress message bus address for registered services.
     * @param clientConnectingAddress message bus address for clients that want to connect a registered service.
     * @param underlyingMessaging messaging system used by the message bus.
     * @param protocolFormatter protocol formatter used for the communication between channels.
     */
    public MessageBusMessagingFactory(String serviceConnctingAddress, String clientConnectingAddress, IMessagingSystemFactory underlyingMessaging, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectorFactory = new MessageBusConnectorFactory(serviceConnctingAddress, clientConnectingAddress, underlyingMessaging);

            // Dispatch events in the same thread as notified from the underlying messaging.
            myOutputChannelThreading = new NoDispatching();
            myInputChannelThreading = myOutputChannelThreading;
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
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IThreadDispatcher aDispatcherAfterMessageDecoded = myDispatchingAfterMessageDecoded.getDispatcher();
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, aDispatcherAfterMessageDecoded, myConnectorFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId, String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IThreadDispatcher aDispatcherAfterMessageDecoded = myDispatchingAfterMessageDecoded.getDispatcher();
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, aDispatcherAfterMessageDecoded, myConnectorFactory);
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
            IThreadDispatcher aDispatcher = myInputChannelThreading.getDispatcher();
            IThreadDispatcher aDispatcherAfterMessageDecoded = myDispatchingAfterMessageDecoded.getDispatcher();
            IInputConnector anInputConnector = myConnectorFactory.createInputConnector(channelId);
            DefaultDuplexInputChannel anInputChannel = new DefaultDuplexInputChannel(channelId, aDispatcher, aDispatcherAfterMessageDecoded, anInputConnector);
            return anInputChannel;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Sets threading mode for input channels.
     * @param inputChannelThreading threading model
     * @return
     */
    public MessageBusMessagingFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        myInputChannelThreading = inputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for input channels.
     * @return
     */
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return myInputChannelThreading;
    }
    
    /**
     * Sets threading mode for output channels.
     * @param outputChannelThreading
     * @return
     */
    public MessageBusMessagingFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        myOutputChannelThreading = outputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for output channels.
     * @return
     */
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return myOutputChannelThreading;
    }
    
    /**
     * Sets maximum time for opening connection with the service via the message bus. Default value is 30 seconds.
     * 
     * When the client opens the connection with a service via message bus it requests message bus to open connection
     * with a desired service. The message checks if the requested service exists and if yes it forwards the open connection request.
     * Then when the service receives the open connection request it sends back the confirmation message that the client is connected.
     * This timeout specifies the maximum time which is allowed for sending the open connection request and receiving the confirmation from the service.
     * 
     * @param milliseconds
     */
    public void setConnectTimeout(int milliseconds)
    {
        myConnectorFactory.setOpenConnectionTimeout(milliseconds);
    }
    
    /**
     * Returns maximum time for opening connection with the service via the message bus. Default value is 30 seconds.
     * 
     * When the client opens the connection with a service via message bus it requests message bus to open connection
     * with a desired service. The message checks if the requested service exists and if yes it forwards the open connection request.
     * Then when the service receives the open connection request it sends back the confirmation message that the client is connected.
     * This timeout specifies the maximum time which is allowed for sending the open connection request and receiving the confirmation from the service.
     * 
     * 
     * @return time in milliseconds
     */
    public int getConnectionTimeout()
    {
        return myConnectorFactory.getOpenConnectionTimeout();
    }
    
    private MessageBusConnectorFactory myConnectorFactory;
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
    private IThreadDispatcherProvider myDispatchingAfterMessageDecoded = new SyncDispatching();
}

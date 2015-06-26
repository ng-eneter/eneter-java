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
 * This messaging provides the client-service communication via the message bus.
 * It ensures the communication via the message bus is transparent and for communicating parts it looks like a normal
 * communication via output and input channel.<br/>
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
    private class MessageBusOutputConnectorFactory implements IOutputConnectorFactory
    {
        public MessageBusOutputConnectorFactory(String clientConnectingAddress, ISerializer serializer)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientConnectingAddress = clientConnectingAddress;
                mySerializer = serializer;
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
                IDuplexOutputChannel aMessageBusOutputChannel = myClientMessaging.createDuplexOutputChannel(myClientConnectingAddress, clientConnectorAddress);
                return new MessageBusOutputConnector(serviceConnectorAddress, mySerializer, aMessageBusOutputChannel, myOpenConnectionTimeout);
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
        
        public IMessagingSystemFactory getClientMessaging()
        {
            return myClientMessaging;
        }
        
        public void setClientMessaging(IMessagingSystemFactory clientMessaging)
        {
            myClientMessaging = clientMessaging;
        }

        private String myClientConnectingAddress;
        private ISerializer mySerializer;
        private IMessagingSystemFactory myClientMessaging;
        
        private int myOpenConnectionTimeout;
    }
    
    private class MessageBusInputConnectorFactory implements IInputConnectorFactory
    {
        public MessageBusInputConnectorFactory(String serviceConnctingAddress, ISerializer serializer)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myServiceConnectingAddress = serviceConnctingAddress;
                mySerializer = serializer;
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
                IDuplexOutputChannel aMessageBusOutputChannel = myServiceMessaging.createDuplexOutputChannel(myServiceConnectingAddress, inputConnectorAddress);
                return new MessageBusInputConnector(mySerializer, aMessageBusOutputChannel);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public IMessagingSystemFactory getServiceMessaging()
        {
            return myServiceMessaging;
        }
        
        public void setServiceMessaging(IMessagingSystemFactory serviceMessaging)
        {
            myServiceMessaging = serviceMessaging;
        }

        private String myServiceConnectingAddress;
        private ISerializer mySerializer;
        private IMessagingSystemFactory myServiceMessaging;
    }
    
   
    /**
     * Constructs the factory.
     * 
     * @param serviceConnctingAddress message bus address intended for services which want to register in the message bus.
     * It can be null if the message bus factory is intended to create only duplex output channels.
     * @param clientConnectingAddress message bus address intended for clients which want to connect a registered service.
     * It can be null if the message bus factory is intended to create only duplex input channels.
     * @param underlyingMessaging messaging system used by the message bus.
     */
    public MessageBusMessagingFactory(String serviceConnctingAddress, String clientConnectingAddress, IMessagingSystemFactory underlyingMessaging)
    {
        this(serviceConnctingAddress, clientConnectingAddress, underlyingMessaging, underlyingMessaging, new MessageBusCustomSerializer());
    }
    
    /**
     * Constructs the factory.
     * 
     * @param serviceConnctingAddress message bus address intended for services which want to register in the message bus.
     * It can be null if the message bus factory is intended to create only duplex output channels.
     * @param clientConnectingAddress message bus address intended for clients which want to connect a registered service.
     * It can be null if the message bus factory is intended to create only duplex input channels.
     * @param underlyingMessaging messaging system used by the message bus.
     * @param serializer serializer which is used to serialize {@link MessageBusMessage} which is internally used for the communication with
     * the message bus.
     */
    public MessageBusMessagingFactory(String serviceConnctingAddress, String clientConnectingAddress, IMessagingSystemFactory underlyingMessaging, ISerializer serializer)
    {
        this(serviceConnctingAddress, clientConnectingAddress, underlyingMessaging, underlyingMessaging, serializer);
    }
    
    public MessageBusMessagingFactory(String serviceConnctingAddress, String clientConnectingAddress, IMessagingSystemFactory serviceUnderlyingMessaging, IMessagingSystemFactory clientUnderlyingMessaging, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ISerializer aSerializer = (serializer == null) ? new MessageBusCustomSerializer() : serializer;
            myOutputConnectorFactory = new MessageBusOutputConnectorFactory(clientConnectingAddress, aSerializer);
            myInputConnectorFactory = new MessageBusInputConnectorFactory(serviceConnctingAddress, aSerializer);
            
            setClientMessaging(clientUnderlyingMessaging);
            setServiceMessaging(serviceUnderlyingMessaging);

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
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, aDispatcherAfterMessageDecoded, myOutputConnectorFactory);
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
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, aDispatcherAfterMessageDecoded, myOutputConnectorFactory);
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
            IInputConnector anInputConnector = myInputConnectorFactory.createInputConnector(channelId);
            DefaultDuplexInputChannel anInputChannel = new DefaultDuplexInputChannel(channelId, aDispatcher, aDispatcherAfterMessageDecoded, anInputConnector);
            return anInputChannel;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Gets messaging used by clients to connect the message bus.
     * @return
     */
    public IMessagingSystemFactory getClientMessaging()
    {
        return myOutputConnectorFactory.getClientMessaging();
    }
    
    /**
     * Sets messaging used by clients to connect the message bus.
     * @param clientMessaging messaging which shall be used clients to connect the message bus.
     * @return
     */
    public MessageBusMessagingFactory setClientMessaging(IMessagingSystemFactory clientMessaging)
    {
        myOutputConnectorFactory.setClientMessaging(clientMessaging);
        return this;
    }
    
    /**
     * Gets messaging used by services to be exposed via the message bus.
     * @return
     */
    public IMessagingSystemFactory getServiceMessaging()
    {
        return myInputConnectorFactory.getServiceMessaging();
    }
    
    /**
     * messaging used by services to be exposed via the message bus.
     * @param serviceMessaging messaging which shall be used by services to expose their API via the message bus.
     * @return
     */
    public MessageBusMessagingFactory setServiceMessaging(IMessagingSystemFactory serviceMessaging)
    {
        myInputConnectorFactory.setServiceMessaging(serviceMessaging);
        return this;
    }
    
    /**
     * Sets threading mode for input channels.
     * @param inputChannelThreading threading model
     * @return this instance of MessageBusMessagingFactory
     */
    public MessageBusMessagingFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        myInputChannelThreading = inputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for input channels.
     * @return thread dispatcher which is used for input channels.
     */
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return myInputChannelThreading;
    }
    
    /**
     * Sets threading mode for output channels.
     * @param outputChannelThreading
     * @return this instance of MessageBusMessagingFactory
     */
    public MessageBusMessagingFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        myOutputChannelThreading = outputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for output channels.
     * @return thread dispatcher which is used for output channels.
     */
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return myOutputChannelThreading;
    }
    
    /**
     * Sets maximum time for opening connection with the service via the message bus. Default value is 30 seconds.
     * 
     * When the client opens the connection with a service via the message bus it requests message bus to open the connection
     * with a desired service. The message checks if the requested service exists and if yes it forwards the open connection request.
     * Then when the service receives the open connection request it sends back the confirmation message that the client is connected.
     * This timeout specifies the maximum time which is allowed for sending the open connection request and receiving the confirmation from the service.
     * 
     * @param milliseconds
     * @return this instance of MessageBusMessagingFactory
     */
    public MessageBusMessagingFactory setConnectTimeout(int milliseconds)
    {
        myOutputConnectorFactory.setOpenConnectionTimeout(milliseconds);
        return this;
    }
    
    /**
     * Returns maximum time for opening connection with the service via the message bus. Default value is 30 seconds.
     * 
     * When the client opens the connection with a service via the message bus it requests message bus to open the connection
     * with a desired service. The message checks if the requested service exists and if yes it forwards the open connection request.
     * Then when the service receives the open connection request it sends back the confirmation message that the client is connected.
     * This timeout specifies the maximum time which is allowed for sending the open connection request and receiving the confirmation from the service.
     * 
     * 
     * @return time in milliseconds
     */
    public int getConnectionTimeout()
    {
        return myOutputConnectorFactory.getOpenConnectionTimeout();
    }
    
    private MessageBusOutputConnectorFactory myOutputConnectorFactory;
    private MessageBusInputConnectorFactory myInputConnectorFactory;
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
    private IThreadDispatcherProvider myDispatchingAfterMessageDecoded = new SyncDispatching();
}

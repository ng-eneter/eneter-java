package Chat;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.SynchronousMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.messaging.nodes.broker.*;
import eneter.messaging.nodes.dispatcher.*;
import eneter.net.system.EventHandler;

public class ChatApp
{
	int chatPort;
	static ChatApp instantiated = null;
	private IDuplexBrokerClient chatClient;
	private IDuplexDispatcher chatDispatcher;
   
    /**
	 * Private constructor to prevent instantiating this utility class.
	 *
	 * @param port Tcp/ip port where ChatApp is going to run
	 */
	protected ChatApp(int port)
	{
	        this.chatPort = port;
	}
	   
	/**
	 * Start the Service.
	 *
	 * @throws Exception
	 */
	public void Run() throws Exception
    {
            // Local messaging connecting the Dispatcher and the Broker.
            IMessagingSystemFactory bdMessaging = new SynchronousMessagingSystemFactory();

            // Create the broker forwarding notifications to subscribed clients.
            IDuplexBrokerFactory brokerFactory = new DuplexBrokerFactory();
            IDuplexBroker chatBroker = brokerFactory.createBroker();

            // Attach the input channel to the broker.
            // Note: The broker will receive via this channel messages from the Dispatcher.
            IDuplexInputChannel aBrokerInputChannel =
                    bdMessaging.createDuplexInputChannel("paChannel");
            chatBroker.attachDuplexInputChannel(aBrokerInputChannel);

            // Create the dispatcher.
            // The dispatcher will receive TCP messages and also local messages and forward them
            // to the broker.
            // (I.e. broker can get messages from local messaging and from TCP at the same time.)
            IDuplexDispatcherFactory aDispatcherFactory = new DuplexDispatcherFactory(bdMessaging);
            chatDispatcher = aDispatcherFactory.createDuplexDispatcher();

            // Add the broker channel to the dispatcher.
            chatDispatcher.addDuplexOutputChannel("paChannel");

            // Attach the input channel receiving messages from the broker client.
            // Note: The channel id is same as the broker client has.
            IDuplexInputChannel aDispatcherLocalInputChannel =
                    bdMessaging.createDuplexInputChannel("paClient");
            chatDispatcher.attachDuplexInputChannel(aDispatcherLocalInputChannel);

            // Create the broker client notifying calculated results to the broker.
            chatClient = brokerFactory.createBrokerClient();
            
            
            // Set the handler method that will receive notification messages
            // coming from the broker.
            chatClient.brokerMessageReceived().subscribe(myOnBrokerMessageReceived);

            // Subscribe in the broker to be notified for
            // broadcast messages and for private messages.
            // e.g. let say the broadcast is *.
            String[] aMessagesOfInterest = { "*", "Eric" };
            chatClient.subscribe(aMessagesOfInterest);
           
           
            
            // Attach the output channel to the broker.
            // Note: The broker client will use this channel to send messages to the dispatcher.
            IDuplexOutputChannel chatBrokerOutputChannel =
                    bdMessaging.createDuplexOutputChannel("paClient");
            chatClient.attachDuplexOutputChannel(chatBrokerOutputChannel);

            // TCP messaging for the communication with the Silverlight client.
            IMessagingSystemFactory TcpMessaging = new TcpMessagingSystemFactory();

            // Note: Silverlight can communicate only on ports: 4502-4532
            IDuplexInputChannel TcpInputChannel =
                    TcpMessaging.createDuplexInputChannel("tcp://0.0.0.0:" + this.chatPort + "/");
            System.out.println("Chat Service running on port: " + this.chatPort);
           
            // Finaly attach the TCP input channel to the dispatcher and start listening
            // to Silverlight clients.
            chatDispatcher.attachDuplexInputChannel(TcpInputChannel);
    }
   
    
    /**
     * The method is called when the message from the broker is received.
     * In our we are subscribe to '*' and 'Eric'.
     * Therefore the broker forwards in case of '*' or 'Eric'.
     * 
     * Note: Please notice, you do not have to do any logic recognizing if the message
     * is for you, or if it is public or private.
     * This logic is done by the broker automatically. The broker will send to clients
     * only those messages they are subscribed for.
     * 
     * @param sender
     * @param e
     */
    private void onChatMessageReceived(Object sender, BrokerMessageReceivedEventArgs e)
    {
        String aSerializedChatMessage = (String) e.getMessage();
        ChatMessage aChatMessage = ChatMessage.Deserialize(aSerializedChatMessage);
        
        // Display the message here ....
    }
   
    /**
	 * Broadcast a message to all suscribed clients.
	 *
	 * @param e ChatMessage to be sent
	 * @return Boolean Has been send or not
	 */
    private Boolean Broadcast(ChatMessage e)
    {
            try
    {
                    chatClient.sendMessage("paMensaje", e.Serialize());
        return true;
    }
    catch (Exception err)
    {
        EneterTrace.error("Sending the Broadcast message failed.", err);
    }
    return false;
    }
   
    /**
	 * Send a message from a receiver to a single defined client.
	 *
	 * @param e ChatMessage to be sent
	 * @return Boolean Has been send or not
	 */
    private Boolean Send(ChatMessage e)
    {
            try
    {
                    chatClient.sendMessage(e.To, e.Serialize());
        return true;
    }
    catch (Exception err)
    {
        EneterTrace.error("Sending the Private message failed.", err);
    }
    return false;
    }

    /**
	 * Handler used to subscribe for incoming messages.
	 *
	 * @return EventHandler for incoming messages
	 */
    private EventHandler<BrokerMessageReceivedEventArgs> myOnBrokerMessageReceived
	        = new EventHandler<BrokerMessageReceivedEventArgs>()
    {
        @Override
        public void onEvent(Object sender, BrokerMessageReceivedEventArgs e)
        {
            onChatMessageReceived(sender, e);
        }
    };
	
   
	/**
	 * Gets an instance of the ChatApp
	 *
	 * @param PortNum Port where the app is going to run
	 * @return ChatApp Created instance
	 * @throws Exception
	 */
    public static ChatApp GetInstance(int PortNum) throws Exception
    {
            if(instantiated == null)
                    instantiated = new ChatApp(PortNum);
            return instantiated;
    }
   
    /**
	 * Stops the service.
	 */
    public void Stop()
    {
            chatClient.detachDuplexOutputChannel();
            chatDispatcher.detachDuplexInputChannel();
    }
}

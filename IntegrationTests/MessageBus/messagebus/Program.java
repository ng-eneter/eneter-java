package messagebus;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import eneter.messaging.messagingsystems.composites.messagebus.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Program
{
    public static void main(String[] args) throws Exception
    {
        // Message Bus will use TCP for the communication.
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        
        // Input channel to listen to services.
        IDuplexInputChannel aServiceInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8045/");
        
        // Input channel to listen to clients.
        IDuplexInputChannel aClientInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8046/");
        
        // Create the message bus.
        IMessageBus aMessageBus = new MessageBusFactory().createMessageBus();
        
        // Attach channels to the message bus and start listening.
        aMessageBus.attachDuplexInputChannels(aServiceInputChannel, aClientInputChannel);
        
        System.out.println("Message bus service is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        
        // Detach channels and stop listening.
        aMessageBus.detachDuplexInputChannels();
    }
}

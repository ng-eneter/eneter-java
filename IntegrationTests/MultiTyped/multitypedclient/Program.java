package multitypedclient;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Program
{
    public static class MyRequestMessage
    {
        public double Number1;
        public double Number2;
    }

    public static void main(String[] args)
    {
        // Create multi-typed sender.
        IMultiTypedMessagesFactory aFactory = new MultiTypedMessagesFactory();
        ISyncMultitypedMessageSender aSender = aFactory.createSyncMultiTypedMessageSender();
        
        try
        {
            // Attach output channel and be able to communicate with the service.
            IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
            IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8033/");
            aSender.attachDuplexOutputChannel(anOutputChannel);
            
            // Request to caltulate two numbers.
            MyRequestMessage aRequestMessage = new MyRequestMessage();
            aRequestMessage.Number1 = 10;
            aRequestMessage.Number2 = 20;
            double aResult = aSender.sendRequestMessage(aRequestMessage, Double.class, MyRequestMessage.class);
            System.out.println(aRequestMessage.Number1 + " + " + aRequestMessage.Number2 + " = " + aResult);
            
            // Request to calculate factorial.
            int aFactorial = aSender.sendRequestMessage((int)6, Integer.class, Integer.class);
            System.out.println("6! = " + aFactorial);
        }
        catch (Exception err)
        {
            EneterTrace.error("Calculating failed.", err);
        }

        // Detach input channel and stop listening to responses.
        aSender.detachDuplexOutputChannel();
    }

}

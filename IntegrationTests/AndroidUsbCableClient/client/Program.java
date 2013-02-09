package client;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.androidusbcablemessagingsystem.AndroidUsbCableMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.*;


public class Program
{
    private static ISyncDuplexTypedMessageSender<String, String> mySender;

    public static void main(String[] args)
    {
        try
        {
            // Use messaging via Android USB cable.
            IMessagingSystemFactory aMessaging = new AndroidUsbCableMessagingFactory();
            
            // Specify that the android application will listen on the port 8090.
            // Note: adb (on PC) and adbd (on Android) will be configured to forward the
            //       the communication to the port 8090.
            IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("8090");
        
            // Create message sender.
            IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory();
            mySender = aSenderFactory.createSyncDuplexTypedMessageSender(String.class, String.class);
            
            // Attach the output channel using Android USB cable and
            // be able to send messages and receive responses.
            mySender.attachDuplexOutputChannel(anOutputChannel);
            
            // Send request and wait for response.
            String aResponse = mySender.sendRequestMessage("Hello");
            
            // Detach output channel and close the connection.
            mySender.detachDuplexOutputChannel();
            
            System.out.println("Android responded: " + aResponse);
        }
        catch (Exception err)
        {
            EneterTrace.error("Error detected.", err);
        }
    }

}

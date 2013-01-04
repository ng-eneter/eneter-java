package client;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.androidusbcablemessagingsystem.AndroidUsbCableMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.*;


public class Program
{
    private static ISyncTypedMessageSender<String, String> mySender;

    /**
     * @param args
     * @throws  
     */
    public static void main(String[] args)
    {
        try
        {
            IMessagingSystemFactory aMessaging = new AndroidUsbCableMessagingFactory();
            IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("8090");
        
            ISyncTypedMessagesFactory aReceiverFactory = new SyncTypedMessagesFactory();
            mySender = aReceiverFactory.createSyncMessageSender(String.class, String.class);
            mySender.attachDuplexOutputChannel(anOutputChannel);
            
            String aResponse = mySender.sendRequestMessage("Hello");
            
            mySender.detachDuplexOutputChannel();
            
            System.out.println("Android responded: " + aResponse);
        }
        catch (Exception err)
        {
            EneterTrace.error("Error detected.", err);
        }
    }

}

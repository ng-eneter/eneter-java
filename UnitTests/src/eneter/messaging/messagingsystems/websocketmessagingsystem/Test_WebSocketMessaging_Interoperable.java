package eneter.messaging.messagingsystems.websocketmessagingsystem;

import helper.*;

import org.junit.Before;

import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.connectionprotocols.EasyProtocolFormatter;

public class Test_WebSocketMessaging_Interoperable extends MessagingSystemBaseTester
{
    @Before
    public void Setup()
    {
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        String aPort = RandomPortGenerator.generate();
        
        MessagingSystemFactory = new WebSocketMessagingSystemFactory(new EasyProtocolFormatter());
        ChannelId = "ws://127.0.0.1:" + aPort + "/";
        
        this.CompareResponseReceiverId = false;
        this.myRequestMessage = new byte[] { (byte)'M', (byte)'E', (byte)'S', (byte)'S', (byte)'A', (byte)'G', (byte)'E' };
        this.myResponseMessage = new byte[] { (byte)'R', (byte)'E', (byte)'S', (byte)'P', (byte)'O', (byte)'N', (byte)'S', (byte)'E' };
        this.myMessage_10MB = RandomDataGenerator.getBytes(10000000);
    }
}

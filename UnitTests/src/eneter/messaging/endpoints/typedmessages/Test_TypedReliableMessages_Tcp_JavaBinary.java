package eneter.messaging.endpoints.typedmessages;

import java.util.Random;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.JavaBinarySerializer;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_TypedReliableMessages_Tcp_JavaBinary extends TypedReliableMessagesBaseTester
{
    @Before
    public void setup() throws Exception
    {
        IMessagingSystemFactory aMessagingSystem = new TcpMessagingSystemFactory();

        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        String aChannelId = "tcp://127.0.0.1:" + Integer.toString(aPort) + "/";
        ISerializer aSerializer = new JavaBinarySerializer();

        Setup(aMessagingSystem, aChannelId, aSerializer);
    }
}

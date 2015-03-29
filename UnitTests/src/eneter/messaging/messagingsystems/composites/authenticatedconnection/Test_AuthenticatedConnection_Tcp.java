package eneter.messaging.messagingsystems.composites.authenticatedconnection;

import java.util.Random;

import org.junit.Before;

import eneter.messaging.dataprocessing.serializing.AesSerializer;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

public class Test_AuthenticatedConnection_Tcp extends AuthenticatedConnectionBaseTester
{
    @Before
    public void setup() throws Exception
    {
        //EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
        //EneterTrace.TraceLog = new StreamWriter("d:/tracefile.txt");

        Random aRandomPort = new Random();
        int aPort = 7000 + aRandomPort.nextInt(1000);
        
        TcpMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();
        ChannelId = "tcp://[::1]:" + Integer.toString(aPort) + "/";

        MessagingSystemFactory = new AuthenticatedMessagingFactory(anUnderlyingMessaging,
            this, // get login
            this, // get handshake response
            this, // get handshake
            this) // authenticate
        .setAuthenticationTimeout(2000);

        myHandshakeSerializer = new AesSerializer("Password123");
    }
}

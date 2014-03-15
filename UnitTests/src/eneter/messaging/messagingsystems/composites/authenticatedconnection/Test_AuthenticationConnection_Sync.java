package eneter.messaging.messagingsystems.composites.authenticatedconnection;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.AesSerializer;
import eneter.messaging.messagingsystems.synchronousmessagingsystem.*;

public class Test_AuthenticationConnection_Sync extends AuthenticatedConnectionBaseTester
{
    @Before
    public void setup() throws Exception
    {
        //EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
        //EneterTrace.TraceLog = new StreamWriter("d:/tracefile.txt");

        SynchronousMessagingSystemFactory anUnderlyingMessaging = new SynchronousMessagingSystemFactory();
        myChannelId = "MyChannel1";

        myMessagingSystemFactory = new AuthenticatedMessagingFactory(anUnderlyingMessaging,
            this, // get login
            this, // get handshake response
            this, // get handshake
            this) // authenticate
        .setAuthenticationTimeout(2000);

        myHandshakeSerializer = new AesSerializer("Password123");
    }
    
    @Ignore
    @Test
    @Override
    public void authenticationTimeout()
    {
        // Not applicable in synchronous messaging.
    }
    
    @Ignore
    @Test
    @Override
    public void Duplex_13_DisconnectFromResponseReceiverConnected()
    {
        // N.A.
    }
}

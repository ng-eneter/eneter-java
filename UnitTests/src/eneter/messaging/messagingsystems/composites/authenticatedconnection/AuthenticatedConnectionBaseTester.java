package eneter.messaging.messagingsystems.composites.authenticatedconnection;

import static org.junit.Assert.*;

import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.*;
import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.*;

public abstract class AuthenticatedConnectionBaseTester extends MessagingSystemBaseTester
    implements IGetLoginMessage, IGetHandshakeResponseMessage, IGetHandshakeMessage, IAuthenticate, IHandleAuthenticationCancelled
{
    @Test(expected = TimeoutException.class)
    public void authenticationTimeout() throws Exception
    {
        IDuplexInputChannel anInputChannel = MessagingSystemFactory.createDuplexInputChannel(ChannelId);
        IDuplexOutputChannel anOutputChannel = MessagingSystemFactory.createDuplexOutputChannel(ChannelId);

        try
        {
            myAuthenticationSleep = 3000;

            anInputChannel.startListening();

            // Client opens the connection.
            anOutputChannel.openConnection();
        }
        finally
        {
            myAuthenticationSleep = 0;

            anOutputChannel.closeConnection();
            anInputChannel.stopListening();
        }
    }
    
    @Test(expected = IllegalStateException.class)
    public void ConnectionNotGranted() throws Exception
    {
        IDuplexInputChannel anInputChannel = MessagingSystemFactory.createDuplexInputChannel(ChannelId);
        IDuplexOutputChannel anOutputChannel = MessagingSystemFactory.createDuplexOutputChannel(ChannelId);

        try
        {
            myConnectionNotGranted = true;

            anInputChannel.startListening();

            // Client opens the connection.
            anOutputChannel.openConnection();
        }
        finally
        {
            myConnectionNotGranted = false;

            anOutputChannel.closeConnection();
            anInputChannel.stopListening();
        }
    }
    
    @Test
    public void authenticationCancelledByClient() throws Exception
    {
        IDuplexInputChannel anInputChannel = MessagingSystemFactory.createDuplexInputChannel(ChannelId);
        IDuplexOutputChannel anOutputChannel = MessagingSystemFactory.createDuplexOutputChannel(ChannelId);

        try
        {
            myClientCancelAuthentication = true;

            anInputChannel.startListening();

            Exception anException = null;
            try
            {
                // Client opens the connection.
                anOutputChannel.openConnection();
            }
            catch (Exception err)
            {
                anException = err;
            }

            assertThat(anException, instanceOf(IllegalStateException.class));

            // Check that the AuthenticationCancelled calleback was called.
            assertTrue(myAuthenticationCancelled);
        }
        finally
        {
            myClientCancelAuthentication = false;
            myAuthenticationCancelled = false;

            anOutputChannel.closeConnection();
            anInputChannel.stopListening();
        }
    }

    @Override
    public Object getLoginMessage(String channelId, String responseReceiverId)
    {
        return "MyLoginName";
    }
    
    @Override
    public Object getHandshakeMessage(String channelId, String responseReceiverId, Object loginMessage)
    {
        // Sleep in case a timeout is needed.
        try
        {
            Thread.sleep(myAuthenticationSleep);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        
        if (myConnectionNotGranted)
        {
            return null;
        }

        if (loginMessage.equals("MyLoginName"))
        {
            return "MyHandshake";
        }

        return null;
    }
    
    @Override
    public Object getHandshakeResponseMessage(String channelId, String responseReceiverId, Object handshakeMessage)
    {
        if (myClientCancelAuthentication)
        {
            return null;
        }
        
        Object aHandshakeResponse = null;
        try
        {
            aHandshakeResponse = myHandshakeSerializer.serialize((String)handshakeMessage, String.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return aHandshakeResponse;
    }
    
    @Override
    public boolean authenticate(String channelId, String responseReceiverId, Object loginMassage, Object handshakeMessage, Object handshakeResponse)
    {
        String aHandshakeResponse = null;
        try
        {
            aHandshakeResponse = myHandshakeSerializer.deserialize(handshakeResponse, String.class);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return handshakeMessage.equals(aHandshakeResponse);
    }
    
    @Override
    public void handleAuthenticationCancelled(String channelId, String responseReceiverId, Object loginMassage)
    {
        myAuthenticationCancelled = true;
    }


    protected ISerializer myHandshakeSerializer;
    protected long myAuthenticationSleep = 0;
    protected boolean myClientCancelAuthentication;
    protected boolean myAuthenticationCancelled;
    protected boolean myConnectionNotGranted;
}

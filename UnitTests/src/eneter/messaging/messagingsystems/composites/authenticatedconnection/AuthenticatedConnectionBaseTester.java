package eneter.messaging.messagingsystems.composites.authenticatedconnection;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.messagingsystems.MessagingSystemBaseTester;
import eneter.messaging.messagingsystems.messagingsystembase.*;

public abstract class AuthenticatedConnectionBaseTester extends MessagingSystemBaseTester
    implements IGetLoginMessage, IGetHandshakeResponseMessage, IGetHandshakeMessage, IAuthenticate
{
    @Test(expected = TimeoutException.class)
    public void authenticationTimeout() throws Exception
    {
        IDuplexInputChannel anInputChannel = myMessagingSystemFactory.createDuplexInputChannel(myChannelId);
        IDuplexOutputChannel anOutputChannel = myMessagingSystemFactory.createDuplexOutputChannel(myChannelId);

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

        if (loginMessage.equals("MyLoginName"))
        {
            return "MyHandshake";
        }

        return null;
    }

    @Override
    public Object getHandshakeResponseMessage(String channelId, String responseReceiverId, Object handshakeMessage)
    {
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
        String aHandshakeResponse = "";
        try
        {
            aHandshakeResponse = myHandshakeSerializer.deserialize(handshakeResponse, String.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return handshakeMessage.equals(aHandshakeResponse);
    }


    protected ISerializer myHandshakeSerializer;
    protected long myAuthenticationSleep = 0;
}

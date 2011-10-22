package eneter.messaging.messagingsystems.simplemessagingsystembase;

import java.security.InvalidParameterException;

import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;

public class SimpleOutputChannel implements IOutputChannel
{
    public SimpleOutputChannel(String channelId, IMessagingSystemBase messagingSystem)
    {
        if (channelId == null || channelId == "")
        {
            // ??? Trace error
            throw new InvalidParameterException("Input parameter channelId is null or empty string.");
        }
        
        myChannelId = channelId;
        myMessagingSystem = messagingSystem;
    }
    
    public String GetChannelId()
    {
        return myChannelId;
    }

    public void SendMessage(Object message)
    {
        try
        {
            myMessagingSystem.SendMessage(myChannelId, message);
        }
        catch (Exception err)
        {
            // ??? Trace error.
            //throw err;
        }
    }
    
    private IMessagingSystemBase myMessagingSystem;
    private String myChannelId;
}

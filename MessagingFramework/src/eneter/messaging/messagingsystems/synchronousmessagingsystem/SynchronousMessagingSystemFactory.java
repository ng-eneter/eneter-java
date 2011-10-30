package eneter.messaging.messagingsystems.synchronousmessagingsystem;

import eneter.messaging.messagingsystems.messagingsystembase.IInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;
import eneter.messaging.messagingsystems.simplemessagingsystembase.IMessagingSystemBase;
import eneter.messaging.messagingsystems.simplemessagingsystembase.SimpleInputChannel;
import eneter.messaging.messagingsystems.simplemessagingsystembase.SimpleMessagingSystem;
import eneter.messaging.messagingsystems.simplemessagingsystembase.SimpleOutputChannel;

public class SynchronousMessagingSystemFactory implements IMessagingSystemFactory
{
    public SynchronousMessagingSystemFactory()
    {
        myMessagingSystem = new SimpleMessagingSystem(new SynchronousMessagingProvider());
    }
    
    public IOutputChannel createOutputChannel(String channelId)
    {
        return new SimpleOutputChannel(channelId, myMessagingSystem);
    }

    public IInputChannel createInputChannel(String channelId)
    {
        return new SimpleInputChannel(channelId, myMessagingSystem);
    }

    private IMessagingSystemBase myMessagingSystem;
}

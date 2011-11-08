package eneter.messaging.messagingsystems.threadmessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.*;

public class ThreadMessagingSystemFactory implements IMessagingSystemFactory
{
    public ThreadMessagingSystemFactory()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessagingSystem = new SimpleMessagingSystem(new ThreadMessagingProvider());
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public IOutputChannel createOutputChannel(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleOutputChannel(channelId, myMessagingSystem);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IInputChannel createInputChannel(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleInputChannel(channelId, myMessagingSystem);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleDuplexOutputChannel(channelId, null, this);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleDuplexOutputChannel(channelId, responseReceiverId, this);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new SimpleDuplexInputChannel(channelId, this);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingSystemBase myMessagingSystem;
}

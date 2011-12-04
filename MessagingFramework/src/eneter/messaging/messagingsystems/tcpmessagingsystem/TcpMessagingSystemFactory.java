package eneter.messaging.messagingsystems.tcpmessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;

public class TcpMessagingSystemFactory implements IMessagingSystemFactory
{

    @Override
    public IOutputChannel createOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new TcpOutputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IInputChannel createInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new TcpInputChannel(channelId);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
    {
        // TODO Auto-generated method stub
        return null;
    }

}

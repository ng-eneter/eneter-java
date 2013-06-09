package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.messaging.dataprocessing.messagequeueing.internal.IInvoker;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;

class DefaultMessagingSystemFactory implements IMessagingSystemFactory
{

    @Override
    public IOutputChannel createOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultOutputChannel(channelId, myProtocolFormatter, myClientConnectorFactory);
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
            return new DefaultInputChannel(channelId, myWorkingThreadInvoker, myProtocolFormatter, myServiceConnectorFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultDuplexOutputChannel(channelId, null, myWorkingThreadInvoker, myProtocolFormatter, myClientConnectorFactory, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, myWorkingThreadInvoker, myProtocolFormatter, myClientConnectorFactory, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultDuplexInputChannel(channelId, myWorkingThreadInvoker, myProtocolFormatter, myServiceConnectorFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IProtocolFormatter<?> myProtocolFormatter;
    private IClientConnectorFactory myClientConnectorFactory;
    private IServiceConnectorFactory myServiceConnectorFactory;
    private IInvoker myWorkingThreadInvoker;
}

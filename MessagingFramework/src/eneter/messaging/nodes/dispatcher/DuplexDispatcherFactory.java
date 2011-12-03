package eneter.messaging.nodes.dispatcher;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;

public class DuplexDispatcherFactory implements IDuplexDispatcherFactory
{
    public DuplexDispatcherFactory(IMessagingSystemFactory duplexOutputChannelsFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessagingSystemFactory = duplexOutputChannelsFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public IDuplexDispatcher createDuplexDispatcher()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexDispatcher(myMessagingSystemFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingSystemFactory myMessagingSystemFactory;
}

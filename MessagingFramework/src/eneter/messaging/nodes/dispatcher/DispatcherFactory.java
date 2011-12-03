package eneter.messaging.nodes.dispatcher;

import eneter.messaging.diagnostic.EneterTrace;

public class DispatcherFactory implements IDispatcherFactory
{

    @Override
    public IDispatcher createDispatcher()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new Dispatcher();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

}

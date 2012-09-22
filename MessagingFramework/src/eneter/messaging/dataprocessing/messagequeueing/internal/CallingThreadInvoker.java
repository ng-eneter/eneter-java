package eneter.messaging.dataprocessing.messagequeueing.internal;

import eneter.net.system.IMethod;

public class CallingThreadInvoker implements IInvoker
{

    @Override
    public void start()
    {
        // Not applicable.
    }

    @Override
    public void stop()
    {
     // Not applicable.
    }

    @Override
    public void invoke(IMethod workItem) throws Exception
    {
        workItem.invoke();
    }

}

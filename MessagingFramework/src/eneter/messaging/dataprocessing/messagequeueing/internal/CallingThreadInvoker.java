/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.messagequeueing.internal;

import eneter.net.system.internal.IMethod;

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

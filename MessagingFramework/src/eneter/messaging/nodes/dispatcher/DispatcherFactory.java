/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements the factory to create the one-way dispatcher.
 */
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

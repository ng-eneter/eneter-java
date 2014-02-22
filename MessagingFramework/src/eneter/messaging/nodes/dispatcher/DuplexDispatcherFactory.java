/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;

/**
 * Implements factory to create the dispatcher.
 *
 */
public class DuplexDispatcherFactory implements IDuplexDispatcherFactory
{
    /**
     * Constructs the duplex dispatcher factory.
     * @param duplexOutputChannelsFactory the messaging system factory used to create duplex output channels
     */
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

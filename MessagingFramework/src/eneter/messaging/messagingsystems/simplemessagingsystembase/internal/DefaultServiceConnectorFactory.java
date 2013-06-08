/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.messaging.diagnostic.EneterTrace;

class DefaultServiceConnectorFactory implements IServiceConnectorFactory
{
    public DefaultServiceConnectorFactory(IMessagingProvider messagingProvider)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessagingProvider = messagingProvider;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public IServiceConnector createServiceConnector(String serviceConnectorAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultServiceConnector(serviceConnectorAddress, myMessagingProvider);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingProvider myMessagingProvider;
}

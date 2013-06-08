/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.messaging.diagnostic.EneterTrace;

class DefaultClientConnectorFactory implements IClientConnectorFactory
{
    public DefaultClientConnectorFactory(IMessagingProvider messagingProvider)
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
    public IClientConnector createClientConnector(
            String serviceConnectorAddress, String clientConnectorAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultClientConnector(serviceConnectorAddress, clientConnectorAddress, myMessagingProvider);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private IMessagingProvider myMessagingProvider;
}

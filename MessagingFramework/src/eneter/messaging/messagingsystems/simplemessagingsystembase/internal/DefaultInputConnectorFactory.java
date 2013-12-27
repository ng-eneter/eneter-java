/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.messaging.diagnostic.EneterTrace;

class DefaultInputConnectorFactory implements IInputConnectorFactory
{
    public DefaultInputConnectorFactory(IMessagingProvider messagingProvider)
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
    public IInputConnector createInputConnector(String serviceConnectorAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultInputConnector(serviceConnectorAddress, myMessagingProvider);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingProvider myMessagingProvider;
}

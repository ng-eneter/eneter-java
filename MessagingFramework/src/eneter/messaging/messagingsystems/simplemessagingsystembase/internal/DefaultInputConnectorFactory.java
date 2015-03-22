/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;

class DefaultInputConnectorFactory implements IInputConnectorFactory
{
    public DefaultInputConnectorFactory(IMessagingProvider messagingProvider, IProtocolFormatter protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myMessagingProvider = messagingProvider;
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public IInputConnector createInputConnector(String inputConnectorAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DefaultInputConnector(inputConnectorAddress, myMessagingProvider, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IMessagingProvider myMessagingProvider;
    private IProtocolFormatter myProtocolFormatter;
}

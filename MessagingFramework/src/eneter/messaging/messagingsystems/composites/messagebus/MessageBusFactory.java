/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;

/**
 * Implements factory for creating the message bus.
 *
 */
public class MessageBusFactory implements IMessageBusFactory
{
    /**
     * Constructs the factory with default parameters.
     * 
     * Default EneterProtocolFormatter is used.
     */
    public MessageBusFactory()
    {
        this(new EneterProtocolFormatter());
    }

    /**
     * Construct the factory.
     * @param protocolFormatter This protocol formatter must be exactly same as is used by both channels that will be attached to the message bus.
     */
    public MessageBusFactory(IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IMessageBus createMessageBus()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new MessageBus(myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private IProtocolFormatter<?> myProtocolFormatter;
}

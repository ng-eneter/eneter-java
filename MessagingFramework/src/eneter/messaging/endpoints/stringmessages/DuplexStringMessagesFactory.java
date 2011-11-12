/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements the factory to create duplex string message sender and receiver.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public class DuplexStringMessagesFactory implements IDuplexStringMessagesFactory
{

    /**
     * Creates the duplex string message sender.
     */
    @Override
    public IDuplexStringMessageSender createDuplexStringMessageSender()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexStringMessageSender();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex string message receiver.
     */
    @Override
    public IDuplexStringMessageReceiver createDuplexStringMessageReceiver()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new DuplexStringMessageReceiver();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
}

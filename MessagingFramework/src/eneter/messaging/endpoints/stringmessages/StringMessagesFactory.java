/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.diagnostic.EneterTrace;


/**
 * The interface declares the factory to create string message senders and receivers.
 *
 */
public class StringMessagesFactory implements IStringMessagesFactory
{

    /**
     * Creates the string message sender.
     */
    @Override
    public IStringMessageSender CreateStringMessageSender()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new StringMessageSender();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the string message receiver.
     */
    @Override
    public IStringMessageReceiver CreateStringMessageReceiver()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new StringMessageReceiver();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

}

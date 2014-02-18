/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

/**
 * Declares factory creating the message bus.
 *
 */
public interface IMessageBusFactory
{
    /**
     * Instantiates the message bus.
     * @return
     */
    IMessageBus createMessageBus();
}

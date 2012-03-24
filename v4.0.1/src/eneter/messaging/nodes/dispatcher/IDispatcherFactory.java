/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

/**
 * Declares the factory creating the one-way dispatcher.
 * 
 * The one-way dispatcher sends messages to all attached output channels.
 *
 */
public interface IDispatcherFactory
{
    /**
     * Creates the dispatcher.
     * @return
     */
    IDispatcher createDispatcher();
}

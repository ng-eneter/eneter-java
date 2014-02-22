/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

/**
 * Creates the dispatcher.
 * 
 * The dispatcher sends messages to all duplex output channels and also can route back response messages.
 */
public interface IDuplexDispatcherFactory
{
    /**
     * Creates the dispatcher.
     * @return duplex dispatcher
     */
    IDuplexDispatcher createDuplexDispatcher();
}

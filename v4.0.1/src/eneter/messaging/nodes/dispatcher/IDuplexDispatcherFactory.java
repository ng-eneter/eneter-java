/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

/**
 * Declares the factory to create the bidirectional dispatcher.
 * 
 * The bidirectional dispatcher sends messages to all duplex output channels and also can route back response messages.
 */
public interface IDuplexDispatcherFactory
{
    /**
     * Creates the duplex dispatcher.
     * @return
     */
    IDuplexDispatcher createDuplexDispatcher();
}

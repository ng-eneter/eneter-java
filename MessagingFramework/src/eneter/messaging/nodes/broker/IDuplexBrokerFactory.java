/**
 * Project: Eneter.Messaging.Framework for Java
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

/**
 * Declares the factory to create the broker and the broker client.
 * @author Ondrej Uzovic
 *
 */
public interface IDuplexBrokerFactory
{
    /**
     * Creates the broker client.
     * The broker client is able to send messages to the broker (via attached duplex output channel).
     * It also can subscribe for messages to receive notifications from the broker.
     * @return
     * @throws Exception
     */
    IDuplexBrokerClient createBrokerClient() throws Exception;
    
    /**
     * Creates the broker.
     * The broker receives messages and forwards them to subscribers.
     * @return
     * @throws Exception
     */
    IDuplexBroker createBroker() throws Exception;
}

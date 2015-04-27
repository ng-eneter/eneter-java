/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * Creates senders and receivers of typed messages.
 *
 */
public interface IDuplexTypedMessagesFactory
{
    /**
     * Creates message sender (client) which can send messages and receive response messages.
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return duplex typed message sender
     */
    <TResponse, TRequest> IDuplexTypedMessageSender<TResponse, TRequest> createDuplexTypedMessageSender(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz);
    
    /**
     * Creates message sender (client) which sends a request message and then waits for the response.
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return synchronous duplex typed message sender
     */
    <TResponse, TRequest> ISyncDuplexTypedMessageSender<TResponse, TRequest> createSyncDuplexTypedMessageSender(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz);
    
    /**
     * Creates message receiver (service) which can receive messages and send back response messages.
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return duplex typed message receiver
     */
    <TResponse, TRequest> IDuplexTypedMessageReceiver<TResponse, TRequest> createDuplexTypedMessageReceiver(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz);
}

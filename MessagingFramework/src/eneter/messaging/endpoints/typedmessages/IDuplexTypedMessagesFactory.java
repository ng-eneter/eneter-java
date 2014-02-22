/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
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
     * Creates duplex typed message sender that can send request messages and receive response messages of specified type.
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return duplex typed message sender
     */
    <TResponse, TRequest> IDuplexTypedMessageSender<TResponse, TRequest> createDuplexTypedMessageSender(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz);
    
    /**
     * Creates synchronous duplex typed message sender that sends a request message and then
     * waits until the response message is received.
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return synchronous duplex typed message sender
     */
    <TResponse, TRequest> ISyncDuplexTypedMessageSender<TResponse, TRequest> createSyncDuplexTypedMessageSender(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz);
    
    /**
     * Creates duplex typed message receiver that can receive request messages and
     * send back response messages of specified type.
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return duplex typed message receiver
     */
    <TResponse, TRequest> IDuplexTypedMessageReceiver<TResponse, TRequest> createDuplexTypedMessageReceiver(Class<TResponse> responseMessageClazz, Class<TRequest> requestMessageClazz);
}

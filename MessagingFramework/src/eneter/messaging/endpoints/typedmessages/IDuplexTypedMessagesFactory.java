/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * Declares the factory to create typed message senders and receivers.
 *
 */
public interface IDuplexTypedMessagesFactory
{
    /**
     * Creates duplex typed message sender that can send request messages and receive response
     * messages of specified type.
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return
     */
    <_ResponseType, _RequestType> IDuplexTypedMessageSender<_ResponseType, _RequestType> createDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz);
    
    /**
     * Creates synchronous duplex typed message sender that sends a request message and then
     * waits until the response message is received.
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return
     */
    <_ResponseType, _RequestType> ISyncDuplexTypedMessageSender<_ResponseType, _RequestType> createSyncDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz);
    
    /**
     * Creates duplex typed message receiver that can receive request messages and
     * send back response messages of specified type.
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return
     */
    <_ResponseType, _RequestType> IDuplexTypedMessageReceiver<_ResponseType, _RequestType> createDuplexTypedMessageReceiver(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz);
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * The interface declares the factory to create reliable typed message sender and receiver.
 *
 */
public interface IReliableTypedMessagesFactory
{
    /**
     * Creates reliable typed message sender.
     * 
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return reliable typed message sender
     */
    <_ResponseType, _RequestType> IReliableTypedMessageSender<_ResponseType, _RequestType> createReliableDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz);
    
    /**
     * Creates reliable typed message receiver.
     * 
     * @param responseMessageClazz type of response messages
     * @param requestMessageClazz type of request messages
     * @return reliable typed message receiver
     */
    <_ResponseType, _RequestType> IReliableTypedMessageReceiver<_ResponseType, _RequestType> createReliableDuplexTypedMessageReceiver(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz);
}

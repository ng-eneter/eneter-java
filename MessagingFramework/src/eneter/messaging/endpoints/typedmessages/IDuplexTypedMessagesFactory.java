/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * The interface declares the factory to create duplex strongly typed message sender and receiver.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public interface IDuplexTypedMessagesFactory
{
    /**
     * Creates duplex typed message sender.
     * @return
     */
    <_ResponseType, _RequestType> IDuplexTypedMessageSender<_ResponseType, _RequestType> createDuplexTypedMessageSender(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz);
    
    /**
     * Creates duplex typed message receiver.
     * @return
     */
    <_ResponseType, _RequestType> IDuplexTypedMessageReceiver<_ResponseType, _RequestType> createDuplexTypedMessageReceiver(Class<_ResponseType> responseMessageClazz, Class<_RequestType> requestMessageClazz);
}

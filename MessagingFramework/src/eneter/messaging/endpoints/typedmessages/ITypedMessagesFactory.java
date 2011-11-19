/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

/**
 * The interface declares the factory to create strongly typed message senders and receivers.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public interface ITypedMessagesFactory
{
    /**
     * Creates the typed message sender.
     * The sender sends the messages via attached one-way output channel.
     * @param messageClazz represents the type of the message for the serialization/deserialization purposes.
     * @return
     */
    <_MessageDataType> ITypedMessageSender<_MessageDataType> createTypedMessageSender(Class<_MessageDataType> messageClazz);
    
    /**
     * Creates the typed message receiver.
     * The receiver receives messages via attached one-way input channel.
     * @param messageClazz represents the type of the message for the serialization/deserialization purposes.
     * @return
     */
    <_MessageDataType> ITypedMessageReceiver<_MessageDataType> createTypedMessageReceiver(Class<_MessageDataType> messageClazz);
}

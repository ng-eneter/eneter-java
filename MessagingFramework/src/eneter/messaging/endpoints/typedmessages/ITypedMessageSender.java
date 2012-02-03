/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableOutputChannel;

/**
 * The interface declares the strongly typed messsage sender.
 * The sender is able to send messages of the specified type via one-way output channel.
 * @author Ondrej Uzovic & Martin Valach
 *
 * @param <_MessageData> type representing the message.
 */
public interface ITypedMessageSender<_MessageData> extends IAttachableOutputChannel
{
    /**
     * Sends the message of specified type via IOutputChannel.
     * @param messageData message to be send
     * @throws Exception 
     */
    void sendMessage(_MessageData messageData) throws Exception;
}

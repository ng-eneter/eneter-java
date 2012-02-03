/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.infrastructure.attachable.IAttachableOutputChannel;

/**
 * The interface declares the string message sender.
 * The sender is able to send text messages via one-way output channel.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public interface IStringMessageSender extends IAttachableOutputChannel
{
    void sendMessage(String message) throws Exception;
}

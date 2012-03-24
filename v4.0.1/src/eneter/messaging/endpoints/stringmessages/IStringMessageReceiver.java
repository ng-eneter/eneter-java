/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.infrastructure.attachable.IAttachableInputChannel;
import eneter.net.system.Event;

/**
 * The interface declares the string message receiver.
 * The receiver is able to receive text messages via one-way input channel.
 *
 */
public interface IStringMessageReceiver extends IAttachableInputChannel
{
    /**
     * The event is invoked when a string message was received.
     * @return
     */
    public Event<StringMessageEventArgs> messageReceived();
}

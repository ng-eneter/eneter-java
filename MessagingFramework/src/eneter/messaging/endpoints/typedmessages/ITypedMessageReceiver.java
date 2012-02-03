/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import eneter.messaging.infrastructure.attachable.IAttachableInputChannel;
import eneter.net.system.Event;

/**
 * The interface declares the strongly typed message receiver.
 * The receiver is able to receive messages of the specified type via one-way input channel.
 * @author Ondrej Uzovic & Martin Valach
 *
 * @param <_MessageDataType> type of the message
 */
public interface ITypedMessageReceiver<_MessageDataType> extends IAttachableInputChannel
{
    /**
     * The event is invoked when a typed message was received.
     * @return
     */
    Event<TypedMessageReceivedEventArgs<_MessageDataType>> messageReceived();
}

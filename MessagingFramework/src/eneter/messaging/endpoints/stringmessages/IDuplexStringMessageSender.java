/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.net.system.Event;

/**
 * The interface declares the duplex string message sender.
 * The duplex sender is able to send text messages and receive text responses.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public interface IDuplexStringMessageSender extends IAttachableDuplexOutputChannel
{
    /**
     * The event is invoked when a response message from duplex string message receiver was received.
     * @return
     */
    Event<StringResponseReceivedEventArgs> responseReceived();

    /**
     * Sends the message via the attached duplex output channel.
     * @param message
     * @throws Exception 
     */
    void sendMessage(String message) throws Exception;
}

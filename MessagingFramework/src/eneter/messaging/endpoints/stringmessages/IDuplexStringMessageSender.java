/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.Event;

/**
 * Sender of text messages.
 *
 */
public interface IDuplexStringMessageSender extends IAttachableDuplexOutputChannel
{
    /**
     * The event is raised when the connection with receiver is opened.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionOpened();
    
    /**
     * The event is raised when the connection with receiver is closed.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    /**
     * The event is raised when a response message is received.
     * @return
     */
    Event<StringResponseReceivedEventArgs> responseReceived();

    /**
     * Sends the text message to the response receiver.
     * @param message text message.
     * @throws Exception
     */
    void sendMessage(String message) throws Exception;
}

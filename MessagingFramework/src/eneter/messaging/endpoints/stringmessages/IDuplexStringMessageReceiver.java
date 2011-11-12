/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

/**
 * The interface declares the reliable string message receiver.
 * The reliable string message receiver can receiver string messages and response string messages.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public interface IDuplexStringMessageReceiver extends IAttachableDuplexInputChannel
{
    /**
     * The event is invoked when the message was received.
     * @return
     */
    Event<StringRequestReceivedEventArgs> requestReceived();
    
    /**
     * The event is invoked when a duplex string message sender opened the connection.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    /**
     * The event is invoked when a duplex string message sender closed the connection.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
    
    /**
     * Sends the response message back to the duplex string message sender.
     * @param responseReceiverId identifies the duplex string message sender that will receive the response
     * @param responseMessage response message
     * @throws Exception 
     */
    void sendResponseMessage(String responseReceiverId, String responseMessage) throws Exception;
}

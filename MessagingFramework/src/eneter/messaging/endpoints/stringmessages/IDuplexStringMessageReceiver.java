/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.stringmessages;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

/**
 * Declares the duplex message receiver which can receive text messages and send back text response messages.
 *
 */
public interface IDuplexStringMessageReceiver extends IAttachableDuplexInputChannel
{
    /**
     * The event is raised when a text message is received.
     * @return
     */
    Event<StringRequestReceivedEventArgs> requestReceived();
    
    /**
     * The event is raised when a duplex string message sender opened the connection.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    /**
     * The event is raised when a duplex string message sender closed the connection.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
    
    /**
     * Sends the response message back to the string message sender.
     * @param responseReceiverId identifies the string message sender that shall receive the response
     * @param responseMessage response text message
     * @throws Exception 
     */
    void sendResponseMessage(String responseReceiverId, String responseMessage) throws Exception;
}

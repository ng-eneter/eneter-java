/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

/**
 * Callback method to get the handshake message.
 * 
 * When AuthenticatedDuplexInputChannel receives the login message this callback is called to get
 * the handshake message.
 * The handshake message is then sent to the connecting AuthenticatedDuplexOutputChannel which will process it
 * and send back the handshake response message.
 *
 */
public interface IGetHandshakeMessage
{
    /**
     * Returns the handshake message.
     * 
     * Returned handshake message must be String or byte[].
     * If it returns null it means the connection will be closed. (e.g. if the login message was not accepted.)
     * 
     * @param channelId connection address
     * @param responseReceiverId unique id representing the connection with the client
     * @param loginMessage login name used by the client
     * @return handshake message (must be String or byte[])
     */
    Object getHandshakeMessage(String channelId, String responseReceiverId, Object loginMessage);
}

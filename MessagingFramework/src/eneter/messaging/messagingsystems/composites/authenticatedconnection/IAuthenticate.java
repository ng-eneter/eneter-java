/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

/**
 * Callback method to authenticate the connection.
 * 
 * When AuthenticatedDuplexInputChannel receives the handshake response message it performs the authentication of the connection.
 *
 */
public interface IAuthenticate
{
    /**
     * Performs the authentication.
     * When AuthenticatedDuplexInputChannel receives the handshake response message it performs the authentication of the connection.<br/>
     * If it returns true the connection will be established.
     * If it returns false the connection will be closed.
     * 
     * @param channelId service address.
     * @param responseReceiverId unique id representing the connection with the client. 
     * @param loginMessage login message that was sent from the client
     * @param handshakeMessage verification message (question) that service sent to the client. 
     * @param handshakeResponseMessage client's response to the handshake message.
     * @return true if the authentication passed and the connection can be established.
     */
    boolean authenticate(String channelId, String responseReceiverId, Object loginMessage, Object handshakeMessage, Object handshakeResponseMessage);
}

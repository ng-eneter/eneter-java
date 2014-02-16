/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

/**
 * Callback method to get the response message for the handshake message.
 * 
 * When AuthenticatedDuplexOutputChannel receives the handshake message it calls this callback to get
 * the response message for the handshake message.
 * The response handshake message is then sent to AuthenticatedDuplexInputChannel which will
 * then authenticate the connection.
 *
 */
public interface IGetHandshakeResponseMessage
{
    /**
     * Returns response for the handshake message.
     * 
     * Returned response message must be String or byte[].
     * If it returns null then it means the handshake message is not accepted and the connection will be closed.
     * 
     * @param channelId service address
     * @param responseReceiverId unique unique id representing the connection with the client
     * @param handshakeMessage handshake message received from the service
     * @return handshake response message (must be String or byte[]) 
     */
    Object getHandshakeResponseMessage(String channelId, String responseReceiverId, Object handshakeMessage);
}

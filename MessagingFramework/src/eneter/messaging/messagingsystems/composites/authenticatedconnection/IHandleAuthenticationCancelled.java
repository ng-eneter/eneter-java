/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

/**
 * Callback method to handle when the output channel closes the connection during the authentication sequence.
 * 
 * The callback method is called by AuthenticatedDuplexInputChannel when the AuthenticatedDuplexOutputChannel closes the connection during the authentication sequence.
 * It allows the user code to detect a canceled authentication and clean resources.
 */
public interface IHandleAuthenticationCancelled
{
    /**
     * Callback method to handle when the output channel closes the connection during the authentication sequence.
     * 
     * @param channelId service address
     * @param responseReceiverId unique id representing the connection with the client
     * @param loginMessage login message that was sent from the client
     */
    void handleAuthenticationCancelled(String channelId, String responseReceiverId, Object loginMessage);
}

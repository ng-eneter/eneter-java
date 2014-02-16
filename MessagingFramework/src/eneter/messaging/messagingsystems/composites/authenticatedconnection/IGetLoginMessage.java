/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

/**
 * Callback providing the login message.
 * 
 * This callback is called from the AuthenticatedDuplexOutputChannel when it wants to create the connection.
 * Returned login message is then sent to AuthenticatedDuplexInputChannel. 
 *
 */
public interface IGetLoginMessage
{
    /**
     * Returns the login name.
     * 
     * Returned login message must be String or byte[].
     * 
     * @param channelId service address
     * @param responseReceiverId unique id representing the connection with the client
     * @return login message (must be String or byte[])
     */
    Object getLoginMessage(String channelId, String responseReceiverId);
}

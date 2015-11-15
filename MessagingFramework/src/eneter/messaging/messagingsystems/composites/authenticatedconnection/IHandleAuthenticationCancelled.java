/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

public interface IHandleAuthenticationCancelled
{
    void handleAuthenticationCancelled(String channelId, String responseReceiverId, Object loginMessage);
}

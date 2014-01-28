/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

public interface IAuthenticate
{
    boolean authenticate(String channelId, String responseReceiverId, Object loginMessage, Object handshakeMessage, Object handshakeResponseMessage);
}

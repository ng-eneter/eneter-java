/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

public interface IGetHandshakeMessage
{
    Object getHandshakeMessage(String channelId, String responseReceiverId, Object loginMessage);
}

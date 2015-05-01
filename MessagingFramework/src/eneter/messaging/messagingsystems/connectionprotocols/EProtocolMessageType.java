/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.connectionprotocols;

/**
 * Type of the message sent between channels.
 * 
 */
public enum EProtocolMessageType
{
    /**
     * Open connection message.
     */
    OpenConnectionRequest,
    
    /**
     * Close connection message.
     */
    CloseConnectionRequest,
    
    /**
     * Request message or response message.
     */
    MessageReceived
}

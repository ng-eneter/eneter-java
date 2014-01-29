/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.connectionprotocols;

/**
 * Indicates the type of the low-level protocol message.
 * 
 */
public enum EProtocolMessageType
{
    /**
     * Unknown message.
     */
    Unknown,
    
    /**
     * Open connection request message.
     */
    OpenConnectionRequest,
    
    /**
     * Close connection request message.
     */
    CloseConnectionRequest,
    
    /**
     * Message or reaponse message.
     */
    MessageReceived
}

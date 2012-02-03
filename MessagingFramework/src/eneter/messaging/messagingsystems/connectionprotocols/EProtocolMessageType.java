/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.connectionprotocols;

public enum EProtocolMessageType
{
    Unknown,
    OpenConnectionRequest,
    CloseConnectionRequest,
    PollRequest,
    MessageReceived
}

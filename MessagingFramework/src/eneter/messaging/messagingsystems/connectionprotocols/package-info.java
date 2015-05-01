/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Encoding/decoding the communication between channels.
 * 
 * The protocol formatter is responsible for encoding/decoding the communication between output and input channel.
 * There are three messages the protocol formatter needs to encode/decode:
 * <ul>
 * <li><b>Open Connection</b> - when output channel asks input channel to open the connection.</li>
 * <li><b>Close Connection</b> - when output channel asks input channel to close the connection. Or when input channel
 *                               disconnects the output channel.</li>
 * <li><b>Message</b> - when output and input channel sends request or response message.</li>
 * </ul> 
 */
package eneter.messaging.messagingsystems.connectionprotocols;
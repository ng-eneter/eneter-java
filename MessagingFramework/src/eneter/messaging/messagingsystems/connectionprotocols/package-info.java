/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Encoding/decoding the communication between communicating channels.
 * 
 * The protocol formatter is responsible for encoding these three types of messages that are used
 * for the interaction between output and input channel. 
 * <ul>
 * <li><b>Open Connection</b> - Output channel sends this message to the input channel when it opens the connection.</li>
 * <li><b>Close Connection</b> - Output channel sends this message to the input channel when it closes the connection.
 *                        The input channel sends this message to the output channel when it disconnects the output channel. </li>
 * <li><b>Message</b> - output channel uses this message when it sends a data message (request message) to the input channel.
 *               The input channel sends this message when it sends a data response message back to the output channel.</li>
 * </ul> 
 */
package eneter.messaging.messagingsystems.connectionprotocols;
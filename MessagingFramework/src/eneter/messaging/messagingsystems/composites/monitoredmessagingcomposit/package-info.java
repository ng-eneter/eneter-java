/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


/**
 * Extension providing monitoring the connection.
 * 
 * The monitoring is realized by sending 'ping' messages and receiving 'ping' responses.
 * If the sending of the 'ping' fails or the 'ping' response is not received within the specified
 * time, the connection is considered to be broken.
 */
package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;
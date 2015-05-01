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
 * The monitoring is realized by sending and receiving 'ping' messages within the specified time.
 * If the sending of the 'ping' fails or the 'ping' response is not received within the specified
 * time the connection is considered to be broken.
 */
package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;
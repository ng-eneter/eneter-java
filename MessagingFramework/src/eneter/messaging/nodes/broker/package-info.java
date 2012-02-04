/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Functionality for publish-subscribe scenarios. Clients can subscribe for notification messages.
 * 
 * The broker is intended for publish-subscribe scenarios. Clients can use the broker to subscribe for messages
 * or for sending of notification messages.<br/>
 * The broker works like this:<br/>
 * The client has some event that wants to notify to everybody who is interested. It sends the message to the broker.
 * The broker receives the message and forwards it to everybody who is subscribed for such event.
 */
package eneter.messaging.nodes.broker;
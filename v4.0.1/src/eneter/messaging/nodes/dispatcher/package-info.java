/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Functionality for the component forwarding messages to all attached receivers.
 * 
 * Receives messages and forwards them to all attached receivers.
 * The message is then processed by more services in parallel.
 * E.g. If a client needs to evaluate results from more different services.
 */
package eneter.messaging.nodes.dispatcher;
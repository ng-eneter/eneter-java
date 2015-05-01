/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Communication routing messages into one working thread.
 * 
 * The messaging system transferring messages to a working thread.
 * Received messages are stored in the queue which is then processed by one working thread.
 * Therefore the messages are processed synchronously but it does not block receiving. 
 */
package eneter.messaging.messagingsystems.threadmessagingsystem;
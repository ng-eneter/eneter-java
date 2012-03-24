/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * The messaging system transferring messages synchronously in the context of the caller thread.
 * 
 * This messaging system transfers messages synchronously in the context of the calling thread.
 * Therefore the calling thread is blocked until the message is delivered and processed.
 * However, the notification events (e.g. connection opened, ...) can come in a different thread.
 * The messaging system is very fast and is suitable to deliver messages locally between internal communication components.
 */
package eneter.messaging.messagingsystems.synchronousmessagingsystem;
/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


/**
 * Helper to queue and process messages with a working thread.
 * 
 * Several threads can put messages to the queue and one thread removes them and calls a call-back method to process them.
 */
package eneter.messaging.dataprocessing.messagequeueing;
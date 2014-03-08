/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

/**
 * Receiving messages and events according to specified thread mode. 
 *
 * Treading dispatching allows to specify in which threads received messages shall be received.  
 * E.g. you can specify that events like MessageReceived, ConnectionOpened, ConnectionClosed, ... will be raised in the main UI thread.
 *
 */
package eneter.messaging.threading.dispatching;
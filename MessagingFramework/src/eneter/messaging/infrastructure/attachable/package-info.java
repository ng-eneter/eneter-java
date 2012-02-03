/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


/**
 * Interfaces used by components to be able to attach channels.
 * 
 * In order to be able to send messages, all communication components must be able to attach channels.
 * E.g. if a component needs to send messages and receive responses then it must implement
 * IAttachableDuplexOutputChannel to be able to attach IDuplexOutputChannel.
 */
package eneter.messaging.infrastructure.attachable;
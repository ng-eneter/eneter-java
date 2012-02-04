/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Event handler used to subscribe to events.
 *
 * @param <T> type of the event parameter.
 */
public interface EventHandler<T> extends IMethod2<Object, T>
{
}

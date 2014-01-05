/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Event handler to process events.
 * The event handler is used by a client to subscribe for some event.
 * Then, when the event is notified, the method onEvent is called.<br/>
 * <br/>
 * For example see {@link EventImpl}.
 *
 * @param <T> type of data notified by the event.
 */
public interface EventHandler<T>
{
    /**
     * Method processing the event.
     * 
     * 
     * @param sender
     * @param e
     */
    void onEvent(Object sender, T e);
}

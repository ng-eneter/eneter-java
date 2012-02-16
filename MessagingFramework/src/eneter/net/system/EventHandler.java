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
 *
 * @param <T> type of the event parameter.
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

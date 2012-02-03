/**
 * Project: Eneter.Messaging.Framework for Java
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Emulates the event mechanism from C#.
 * It provides functionality to subscribe and unsubscribe from the event.
 *
 * @param <T> type of the data notified by the event.
 */
public interface Event<T>
{
    /**
     * Subscribes client to for the event.
     * @param eventHandler Event handler provided by the client.
     */
    public void subscribe(EventHandler<T> eventHandler);
    
    /**
     * Unsubscribes client from the event.
     * @param eventHandler Event handler that should be unsubscribed from the event.
     */
    public void unsubscribe(EventHandler<T> eventHandler);
}

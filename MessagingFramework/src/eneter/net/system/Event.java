/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Event mechanism like in C#.
 * It provides functionality to subscribe and unsubscribe from the event.<br/>
 * <br/>
 * For example see {@link EventImpl}.
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

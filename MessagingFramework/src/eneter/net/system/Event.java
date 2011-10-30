/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Emulates the event mechanism from C#.
 * It provides functionality to subscribe and unsubscribe from the event.
 *
 * @param <T> type of the data notified by the event.
 */
public class Event<T>
{
    /**
     * Constructs the event.
     * @param eventImpl internal representation of the event. The internal representation contains functionality
     * such as rising the event that shall not be visible to subscribers.
     */
    public Event(EventImpl<T> eventImpl)
    {
        myEventTrigger = eventImpl;
    }
    
    /**
     * Subscribes client to for the event.
     * @param eventHandler Event handler provided by the client.
     */
    public void subscribe(IMethod2<Object, T> eventHandler)
    {
        myEventTrigger.subscribe(eventHandler);
    }
    
    /**
     * Unsubscribes client from the event.
     * @param eventHandler Event handler that should be unsubscribed from the event.
     */
    public void unsubscribe(IMethod2<Object, T> eventHandler)
    {
        myEventTrigger.unsubscribe(eventHandler);
    }
    
    private EventImpl<T> myEventTrigger;
}

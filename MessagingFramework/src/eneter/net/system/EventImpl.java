/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;

import eneter.messaging.endpoints.typedmessages.TypedResponseReceivedEventArgs;

/**
 * Implements event similar way as in .NET.
 * 
 * The class is intended to be used by a class that wants to raise events.
 * 
 * <pre>
 * {@code
 * // Class exposing some event.
 * class MyExposingClass
 * {
 *      // Exposed to a user for subscribing.
 *      public Event&lt;TMyEvent&gt; calculationCompleted()
 *      {
 *          // Returns event, so that the user can only subscribe or unsubscribe.
 *          // Note: User of the event cannot raise the event.
 *          return myCalculationCompletedEvent.getApi();
 *      }
 *      
 *      ...
 *      
 *      private void someMethod()
 *      {
 *          // Raise the event.
 *          myCalculationCompletedEvent.raise(this, new TMyEvent(...));
 *      }
 *      
 *      // Declaring the event.
 *      private EventImpl&lt;TMyEvent&gt; myCalculationCompletedEvent = new EventImpl&lt;TMyEvent&gt;(); 
 * }
 *
 * ...
 *
 * // Class consuming the event.
 * class MyConsumingClass
 * {
 *      // Subscribing for the event.
 *      private void someMethod()
 *      {
 *          myExposingClass.subscribe(myOnCalculationCompleted);
 *      }
 *      
 *      // Method processing the event.
 *      private void onCalculationCompleted(object sender, TMyEvent e)
 *      {
 *          ...
 *      }
 *      
 *      // Declaring the event handler.
 *      private EventHandler&lt;TMyEvent&gt; myOnCalculationCompleted = new EventHandler&lt;TMyEvent&gt;()
 *      {
 *          public void invoke(Object x, TMyEvent y)
 *                  throws Exception
 *          {
 *              onCalculationCompleted(x, y);
 *          }
 *      }
 * }
 * 
 * }
 * </pre>
 *
 * @param <T>
 */
public class EventImpl<T>
{
    /**
     * Raises the event to all subscribers.
     * 
     * @param sender reference to the sender
     * @param eventArgs event parameter
     * @throws Exception
     */
    public void raise(Object sender, T eventArgs)
            throws Exception
    {
        ArrayList<IMethod2<Object, T>> aSafeCopyOfSubscribers;
        
        synchronized (mySubscribedEventHandlers)
        {
            aSafeCopyOfSubscribers = new ArrayList<IMethod2<Object,T>>(mySubscribedEventHandlers); 
        }
        
        // Use the safe copy to iterate via subscribed observers and notify them about the event.
        Iterator<IMethod2<Object, T>> it = aSafeCopyOfSubscribers.iterator();
        while (it.hasNext())
        {
            IMethod2<Object, T> anEventHandler = it.next();
            
            // Notify the subscriber.
            anEventHandler.invoke(sender, eventArgs);
        }
    }
    
    /**
     * Returns event for the user. The user can subscribe/unsubscribe.
     * @return
     */
    public Event<T> getApi()
    {
        return myEventApi;
    }
    
    /**
     * REturns true if somebody is subscribe.
     * @return
     */
    public boolean isSubscribed()
    {
        synchronized (mySubscribedEventHandlers)
        {
            return !mySubscribedEventHandlers.isEmpty();
        }
    }
    
    private void subscribeClient(EventHandler<T> eventHandler)
    {
        if (eventHandler == null)
        {
            throw new InvalidParameterException("The input parameter eventHandler is null.");
        }
        
        synchronized (mySubscribedEventHandlers)
        {
            // Store the event handler.
            mySubscribedEventHandlers.add(eventHandler);
        }
    }
    
    private void unsubscribeClient(EventHandler<T> eventHandler)
    {
        if (eventHandler == null)
        {
            throw new InvalidParameterException("The input parameter eventHandler is null.");
        }
        
        synchronized (mySubscribedEventHandlers)
        {
            // Remove event handler if it is there. 
            mySubscribedEventHandlers.remove(eventHandler);
        }
    }
    
    private ArrayList<EventHandler<T>> mySubscribedEventHandlers = new ArrayList<EventHandler<T>>();
    
    private Event<T> myEventApi = new Event<T>()
    {
        @Override
        public void subscribe(EventHandler<T> eventHandler)
        {
            subscribeClient(eventHandler);
        }

        @Override
        public void unsubscribe(EventHandler<T> eventHandler)
        {
            unsubscribeClient(eventHandler);
        }
    };
}

package eneter.net.system;


public class Event<T>
{
    public Event(EventImpl<T> eventImpl)
    {
        myEventTrigger = eventImpl;
    }
    
    public void subscribe(IMethod2<Object, T> eventHandler)
    {
        myEventTrigger.subscribe(eventHandler);
    }
    
    public void unsubscribe(IMethod2<Object, T> eventHandler)
    {
        myEventTrigger.unsubscribe(eventHandler);
    }
    
    private EventImpl<T> myEventTrigger;
}

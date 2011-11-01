package eneter.net.system;

public class ClassWithEvent
{
    public Event<String> nameUpdated()
    {
        return myNameUpdatedEvent;
    }
    
    public void SetName(String name) throws Exception
    {
        // Rise the event that the name was updated.
        myNameUpdatedEventImpl.update(this, name);
    }
    
    private EventImpl<String> myNameUpdatedEventImpl = new EventImpl<String>();
    private Event<String> myNameUpdatedEvent = new Event<String>(myNameUpdatedEventImpl);
}

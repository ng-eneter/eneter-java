package eneter.net.system;

public class ClassWithEvent
{
    public Event<String> nameUpdated()
    {
        return myNameUpdatedEventImpl.getApi();
    }
    
    public void SetName(String name) throws Exception
    {
        // Rise the event that the name was updated.
        myNameUpdatedEventImpl.raise(this, name);
    }
    
    private EventImpl<String> myNameUpdatedEventImpl = new EventImpl<String>();
}

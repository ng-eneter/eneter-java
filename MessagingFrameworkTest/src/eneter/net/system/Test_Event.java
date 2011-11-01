package eneter.net.system;

import static org.junit.Assert.*;

import org.junit.Test;

public class Test_Event
{
    /*
     * Tests subscribing and unsubscribing from the event.
     */

    @Test
    public void testSubscribeUnsubscribe()
            throws Exception
    {
        ClassWithEvent aClassWithEvent = new ClassWithEvent();
    
        // Subscribe for the event.
        aClassWithEvent.nameUpdated().subscribe(anEventHandler);
        
        // Use the class with the event, so it invokes the event.
        aClassWithEvent.SetName("Mr. Smith");
        
        // Check the notified event.
        assertEquals("Mr. Smith", myUpdatedName);
        
        // Unsubscribe from the event.
        aClassWithEvent.nameUpdated().unsubscribe(anEventHandler);
        
        myUpdatedName = null;
        
        // Use the class with the event, so it invokes the event.
        aClassWithEvent.SetName("bla bla");
        
        // Check the notified event.
        // Note: Nothing should be received because we are unsubscribed.
        assertNull(myUpdatedName);
    }

    // Event handler.
    private void onNameUpdated(Object sender, String event)
    {
        myUpdatedName = event;
    }
    
    
    // Inner class providing the event handler.
    private IMethod2<Object, String> anEventHandler = new IMethod2<Object, String>()
    {
        @Override
        public void invoke(Object t1, String t2)
        {
            onNameUpdated(t1, t2);
        }
    };
    
    
    private String myUpdatedName;
}

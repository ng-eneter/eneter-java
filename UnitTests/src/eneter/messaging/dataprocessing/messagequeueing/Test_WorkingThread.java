package eneter.messaging.dataprocessing.messagequeueing;

import static org.junit.Assert.*;
import java.util.ArrayList;

import org.junit.Test;

import eneter.net.system.IMethod1;
import eneter.net.system.threading.internal.AutoResetEvent;

public class Test_WorkingThread
{
    @Test
    public void EnqueueDequeueStop()
        throws Exception
    {
        WorkingThread<Object> aWorkingThread = new WorkingThread<Object>();

        final AutoResetEvent aQueueCompleted = new AutoResetEvent(false);
        final ArrayList<String> aReceivedMessages = new ArrayList<String>();
        IMethod1<Object> aProcessingCallback = new IMethod1<Object>()
        {
            @Override
            public void invoke(Object x) throws Exception
            {
                aReceivedMessages.add((String)x);
                
                if (aReceivedMessages.size() == 3)
                {
                    aQueueCompleted.set();
                }
            }
        };
                
        aWorkingThread.registerMessageHandler(aProcessingCallback);

        aWorkingThread.enqueueMessage("Message1");
        aWorkingThread.enqueueMessage("Message2");
        aWorkingThread.enqueueMessage("Message3");

        // Wait until all messages are processed.
        assertTrue("Message queue did not process messages in 1 second", aQueueCompleted.waitOne(1000));
        
        aWorkingThread.unregisterMessageHandler();
        
        assertEquals(3, aReceivedMessages.size());
        assertEquals("Message1", aReceivedMessages.get(0));
        assertEquals("Message2", aReceivedMessages.get(1));
        assertEquals("Message3", aReceivedMessages.get(2));
    }
}

package eneter.messaging.dataprocessing.messagequeueing;

import java.util.ArrayDeque;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.IFunction;



public class MessageQueue<_MessageType>
{

    public void enqueueMessage(_MessageType message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myMessageQueue)
            {
                myMessageQueue.add(message);

                // Release the lock and signal that a message was added to the queue.
                // Note: The signal causes that if there is a blocked thread waitng for a message it is unblocked
                //       and the thread can read and process the message.
                myMessageQueue.notify();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public _MessageType dequeueMessage() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return waitForQueueCall(new IFunction<_MessageType>()
            {
                @Override
                public _MessageType invoke() throws Exception
                {
                    return myMessageQueue.poll();
                }
            });
                   
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public _MessageType peekMessage() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return waitForQueueCall(new IFunction<_MessageType>()
            {
                @Override
                public _MessageType invoke() throws Exception
                {
                    return myMessageQueue.peek();
                }
            });
                   
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void unblockProcessingThreads()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myMessageQueue)
            {
                myIsBlockingMode = false;
                myMessageQueue.notifyAll();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void blockProcessingThreads()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myMessageQueue)
            {
                myIsBlockingMode = true;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public boolean isBlockingMode()
    {
        return myIsBlockingMode;
    }
    
    public int getCount()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myMessageQueue)
            {
                return myMessageQueue.size();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private _MessageType waitForQueueCall(IFunction<_MessageType> func) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myMessageQueue)
            {
                while (myIsBlockingMode && myMessageQueue.isEmpty())
                {
                    // Release the lock and wait for a signal that something is in the queue.
                    // Note: To unblock threads waiting here use UnblockProcesseingThreads().
                    myMessageQueue.wait();
                }

                if (myMessageQueue.isEmpty())
                {
                    return null;
                }

                return func.invoke();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
 
    private ArrayDeque<_MessageType> myMessageQueue = new ArrayDeque<_MessageType>();
    private volatile boolean myIsBlockingMode = true;
}

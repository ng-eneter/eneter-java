/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.messagequeueing;

import java.util.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.internal.IFunction;


/**
 * Queue for messages of type object.
 * 
 * One or more threads can put messages into the queue and other threads
 * can remove them.
 * If the queue is empty the thread reading messages is blocked until a message
 * is put to the queue or the thread is unblocked.
 *
 * @param <_MessageType> Type of the message.
 */
public class MessageQueue<_MessageType>
{

    /**
     * Puts message to the queue.
     * @param message message that shall be enqueued
     */
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
    
    /**
     * Removes the first message from the queue. If the queue is empty the thread is blocked until a message is put to the queue.
     * To unblock waiting threads, use UnblockProcesseingThreads().
     * @return message, it returns null if the waiting thread was unblocked but there is no message in the queue.
     * @throws Exception
     */
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
    
    /**
     * Reads the first message from the queue. If the queue is empty the thread is blocked until a message is put to the queue.
     * To unblock waiting threads, use UnblockProcesseingThreads().
     * @return message, it returns null if the waiting thread was unblocked but there is no message in the queue.
     * @throws Exception
     */
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
    
    /**
     * Releases all threads waiting for messages in DequeueMessage() and sets the queue to the unblocking mode.
     * When the queue is in unblocking mode, the dequeue or peek will not block if data is not available but
     * it will return null.
     */
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
    
    /**
     * Sets the queue to the blocking mode.
     * When the queue is in blocking mode, the dequeue and peek will block until data is available.
     */
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
    
    /**
     * Returns true if the queue blocks threads during dequeue and peek.
     * @return true if the queue is in the blocking mode.
     */
    public boolean isBlockingMode()
    {
        return myIsBlockingMode;
    }
    
    /**
     * Returns number of messages in the queue.
     * @return number of messages in the queue.
     */
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

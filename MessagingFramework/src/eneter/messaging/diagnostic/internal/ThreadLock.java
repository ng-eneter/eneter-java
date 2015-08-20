package eneter.messaging.diagnostic.internal;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;

public class ThreadLock
{
    private class TLockContext
    {
        public int myWaitings;
        public ReentrantLock myLock = new ReentrantLock();
    }
    
    private ThreadLock(Object obj)
    {
        int aNumberOfWaitings;
        
        myLocksLock.lock();
        try
        {
            myLockContext = myLockContexts.get(obj);
            
            // If there is no a thread holding this lock.
            if (myLockContext == null)
            {
                myLockContext = new TLockContext();
                myLockContexts.put(obj, myLockContext);
            }
            else
            {
                // If this is re-acquiring of the lock then leave. 
                if (myLockContext.myLock.isHeldByCurrentThread())
                {
                    return;
                }
            }
            
            // One more waiting thread.
            aNumberOfWaitings = ++myLockContext.myWaitings;
        }
        finally
        {
            myLocksLock.unlock();
        }
        
        // Indicates this is the first acquisition of the lock for this thread.
        myObj = obj;
        
        if (EneterTrace.getDetailLevel() == EDetailLevel.Debug)
        {
            String aMessage = new StringBuilder().append("LOCKING ").append(aNumberOfWaitings).toString();
            EneterTrace.debug(1, aMessage);
        }
        
        
        long aStartAcquiringTime = System.nanoTime();
        
        // Wait until the lock is acquired.
        myLockContext.myLock.lock();
        
        long anElapsedTime = System.nanoTime() - aStartAcquiringTime;
        
        // The thread has acquired the lock so there is one waiting less.
        myLocksLock.lock();
        try
        {
            --myLockContext.myWaitings;
        }
        finally
        {
            myLocksLock.unlock();
        }
        
        if (EneterTrace.getDetailLevel() == EDetailLevel.Debug)
        {
            String anElapsedTimeStr = elapsedTimeToString(anElapsedTime);
            String aMessage = new StringBuilder().append("LOCKED ").append(anElapsedTimeStr).toString();
            EneterTrace.debug(1, aMessage);
        }
        
        if (anElapsedTime >= 1000000000)
        {
            long aMiliseconds = elapsedTimeToMiliseconds(anElapsedTime);
            String aMessage = new StringBuilder().append("Locked after [ms]: ").append(aMiliseconds).toString();
            EneterTrace.warning(1, aMessage);
        }
        
        myLockTime = System.nanoTime();
    }
    
    public static ThreadLock lock(Object obj)
    {
        return new ThreadLock(obj);
    }
    
    public void unlock()
    {
        // If this is the first acquired lock for this thread.
        // (it means if this is not a reentering)
        if (myObj != null)
        {
            long anElapsedTime = System.nanoTime() - myLockTime;
            
            myLocksLock.lock();
            try
            {
                if (myLockContext.myWaitings == 0)
                {
                    myLockContexts.remove(myObj);
                }
            }
            finally
            {
                myLocksLock.unlock();
            }
            
            
            myLockContext.myLock.unlock();
            
            
            if (EneterTrace.getDetailLevel() == EDetailLevel.Debug)
            {
                String anElapsedTimeStr = elapsedTimeToString(anElapsedTime);
                String aMessage = new StringBuilder().append("UNLOCKED ").append(anElapsedTimeStr).toString();
                EneterTrace.debug(1, aMessage);
            }
            
            if (anElapsedTime >= 1000000000)
            {
                long aMiliseconds = elapsedTimeToMiliseconds(anElapsedTime);
                String aMessage = new StringBuilder().append("Unlocked after [ms]: ").append(aMiliseconds).toString();
                EneterTrace.warning(1, aMessage);
            }
        }
    }
    
    private String elapsedTimeToString(long elapsedTime)
    {
        long aHours = (long) (elapsedTime / (60.0 * 60.0 * 1000000000.0));
        elapsedTime -= aHours * 60 * 60 * 1000000000;
        
        long aMinutes = (long) (elapsedTime / (60.0 * 1000000000.0));
        elapsedTime -= aMinutes * 60 * 1000000000;
        
        long aSeconds = elapsedTime / 1000000000;
        elapsedTime -= aSeconds * 1000000000;
        
        float aMiliseconds = elapsedTime / (float)1000000;

        StringBuilder aResult = new StringBuilder()
        .append(aHours).append(":").append(aMinutes).append(":").append(aSeconds).append(".")
        .append(aMiliseconds);
        
        String aTimeStr = aResult.toString();
        return aTimeStr;
    }
    
    private long elapsedTimeToMiliseconds(long elapsedTime)
    {
        long aMiliseconds = elapsedTime / 1000000;
        return aMiliseconds;
    }
    
    private Object myObj;
    private TLockContext myLockContext;
    
    private long myLockTime;
    
    private static final ReentrantLock myLocksLock = new ReentrantLock();
    private static HashMap<Object, TLockContext> myLockContexts = new HashMap<Object, TLockContext>();
}

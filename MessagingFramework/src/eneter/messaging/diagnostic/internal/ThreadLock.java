package eneter.messaging.diagnostic.internal;

import java.util.concurrent.locks.ReentrantLock;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;

public class ThreadLock
{
    public void lock()
    {
        if (myLock.isHeldByCurrentThread())
        {
            myLock.lock();
            return;
        }
        
        if (EneterTrace.getDetailLevel() == EDetailLevel.Debug)
        {
            String aMessage = new StringBuilder().append("LOCKING ").append(myLock.getQueueLength() + 1).toString();
            EneterTrace.debug(1, aMessage);
        }
        
        long aStartAcquiringTime = System.currentTimeMillis();
        myLock.lock();
        long aStopAcquiringTime = System.currentTimeMillis();
        long anElapsedTime = aStopAcquiringTime - aStartAcquiringTime;
        
        if (EneterTrace.getDetailLevel() == EDetailLevel.Debug)
        {
            String anElapsedTimeStr = millisecondsToTimeString(anElapsedTime);
            String aMessage = new StringBuilder().append("LOCKED ").append(anElapsedTimeStr).toString();
            EneterTrace.debug(1, aMessage);
        }
        
        if (anElapsedTime >= 1000)
        {
            String aMessage = new StringBuilder().append("Locked after [ms]: ").append(anElapsedTime).toString();
            EneterTrace.warning(1, aMessage);
        }
        
        myLockTime = aStopAcquiringTime;
    }
    
    public void unlock()
    {
        myLock.unlock();
        
        if (!myLock.isHeldByCurrentThread())
        {
            long anElapsedTime = System.currentTimeMillis() - myLockTime;
            
            if (EneterTrace.getDetailLevel() == EDetailLevel.Debug)
            {
                String anElapsedTimeStr = millisecondsToTimeString(anElapsedTime);
                String aMessage = new StringBuilder().append("UNLOCKED ").append(anElapsedTimeStr).toString();
                EneterTrace.debug(1, aMessage);
            }
            
            if (anElapsedTime >= 1000)
            {
                String aMessage = new StringBuilder().append("Unlocked after [ms]: ").append(anElapsedTime).toString();
                EneterTrace.warning(1, aMessage);
            }
        }
    }
    
    private String millisecondsToTimeString(long milliseconds)
    {
        long aHours = (long) (milliseconds / (60.0 * 60.0 * 1000.0));
        milliseconds -= aHours * 60 * 60 * 1000;
        
        long aMinutes = (long) (milliseconds / (60.0 * 1000.0));
        milliseconds -= aMinutes * 60 * 1000;
        
        long aSeconds = milliseconds / 1000;
        milliseconds -= aSeconds * 1000;
        
        StringBuilder aResult = new StringBuilder()
        .append(aHours).append(":").append(aMinutes).append(":").append(aSeconds).append(".")
        .append(milliseconds);
        
        String aTimeStr = aResult.toString();
        return aTimeStr;
    }
    
   
    
    private long myLockTime;
    private ReentrantLock myLock = new ReentrantLock();
}

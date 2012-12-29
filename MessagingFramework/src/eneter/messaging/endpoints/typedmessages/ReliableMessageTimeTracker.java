/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.util.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;

class ReliableMessageTimeTracker
{
    private class TrackItem
    {
        public TrackItem(String messageId)
        {
        
            myMessageId = messageId;
            SendTime = System.currentTimeMillis();
        }

        public String myMessageId;
        public long SendTime;
    }
    
    public Event<ReliableMessageIdEventArgs> trackingTimeout()
    {
        return myTrackingTimeoutEventImpl.getApi();
    }
    
    public ReliableMessageTimeTracker(int timeout)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myTimeout = timeout;
            myTimer = new Timer("ReliableMessageTimeTracker", true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void AddTracking(String id)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myTrackedMessages)
            {
                // Create the tracking item.
                TrackItem aTrackedMessage = new TrackItem(id);
                myTrackedMessages.add(aTrackedMessage);

                // If the timer is not running then start it.
                if (myTrackedMessages.size() == 1)
                {
                    myTimer.schedule(getTimerTask(), 500);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void RemoveTracking(final String id)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myTrackedMessages)
            {
                try
                {
                    HashSetExt.removeWhere(myTrackedMessages, new IFunction1<Boolean, TrackItem>()
                            {
                                @Override
                                public Boolean invoke(TrackItem x) throws Exception
                                {
                                    return x.myMessageId == id;
                                }
                            });
                }
                catch (Exception err)
                {
                    // This can happen only if there is an error in the source code.
                    EneterTrace.error(TracedObject + "failed to remove a timeouted message.", err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onTimerTick()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            final ArrayList<String> aTimeoutedMessages = new ArrayList<String>();

            final long aCurrentTime = System.currentTimeMillis();

            synchronized (myTrackedMessages)
            {
                try
                {
                    // Remove all timeouted messages from the tracking.
                    HashSetExt.removeWhere(myTrackedMessages, new IFunction1<Boolean, TrackItem>()
                            {
                                @Override
                                public Boolean invoke(TrackItem x) throws Exception
                                {
                                    if (aCurrentTime - x.SendTime > myTimeout)
                                    {
                                        // Store the timeouted id.
                                        aTimeoutedMessages.add(x.myMessageId);
    
                                        // Indicate the tracking can be removed.
                                        return true;
                                    }
    
                                    // Indicate the tracking cannot be removed.
                                    return false;
                                }
                            });
                }
                catch (Exception err)
                {
                    // This can happen only if there is an error in the source code.
                    EneterTrace.error(TracedObject + "failed to remove a timeouted message.", err);
                }
            }

            // Notify timeouted messages.
            for (String aMessageId : aTimeoutedMessages)
            {
                if (myTrackingTimeoutEventImpl.isSubscribed())
                {
                    try
                    {
                        ReliableMessageIdEventArgs aMsg = new ReliableMessageIdEventArgs(aMessageId);
                        myTrackingTimeoutEventImpl.raise(this, aMsg);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject + ErrorHandler.DetectedException, err);
                    }
                }
            }

            // if the timer shall continue
            synchronized (myTrackedMessages)
            {
                if (myTrackedMessages.size() > 0)
                {
                    myTimer.schedule(getTimerTask(), 500);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /*
     * Helper method to get the new instance of the timer task.
     * The problem is, the timer does not allow to reschedule the same instance of the TimerTask
     * and the exception is thrown.
     */
    private TimerTask getTimerTask()
    {
        TimerTask aTimerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                onTimerTick();
            }
        };
        
        return aTimerTask;
    }

    
    
    private Timer myTimer;
    private int myTimeout;
    private HashSet<TrackItem> myTrackedMessages = new HashSet<TrackItem>();
    
    private EventImpl<ReliableMessageIdEventArgs> myTrackingTimeoutEventImpl = new EventImpl<ReliableMessageIdEventArgs>();
    
    String TracedObject = "ReliableMessageTimeTracker ";
}

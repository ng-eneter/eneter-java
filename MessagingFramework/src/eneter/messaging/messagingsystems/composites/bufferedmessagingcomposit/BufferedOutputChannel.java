/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import java.util.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.composites.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.threading.internal.ThreadPool;

class BufferedOutputChannel implements IOutputChannel, ICompositeOutputChannel
{
    public BufferedOutputChannel(IOutputChannel underlyingOutputChannel, long maxOfflineTime)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingOutputChannel = underlyingOutputChannel;
            myMaxOfflineTime = maxOfflineTime;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IOutputChannel getUnderlyingOutputChannel()
    {
        return myUnderlyingOutputChannel;
    }

    @Override
    public String getChannelId()
    {
        return myUnderlyingOutputChannel.getChannelId();
    }

    @Override
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myEnqueuedMessages)
            {
                myEnqueuedMessages.add(message);

                // If the thread sending messages from the queue is not running, then invoke one.
                if (!myThreadIsSendingFlag)
                {
                    myThreadIsSendingFlag = true;

                    Runnable aSender = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                MessageSender();
                            }
                            catch (Exception e)
                            {
                            }
                        }
                    };
                    ThreadPool.queueUserWorkItem(aSender);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private void MessageSender() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Loop taking messages from the queue, until the queue is empty.
            while (true)
            {
                Object aMessage;

                synchronized (myEnqueuedMessages)
                {
                    // If there is a message in the queue, read it.
                    if (myEnqueuedMessages.size() > 0)
                    {
                        aMessage = myEnqueuedMessages.get(0);
                    }
                    else
                    {
                        // There are no messages in the queue, therefore the thread can end.
                        myThreadIsSendingFlag = false;
                        return;
                    }
                }

                // Loop trying to send the message until the send is successsful or timeouted.
                long aStartSendingTime = System.currentTimeMillis();
                while (true)
                {
                    // Try to send the message.
                    try
                    {
                        // Send the message using the underlying output channel.
                        getUnderlyingOutputChannel().sendMessage(aMessage);

                        // The message was successfuly sent, therefore it can be removed from the queue.
                        synchronized (myEnqueuedMessages)
                        {
                            myEnqueuedMessages.remove(0);
                        }

                        // The message was successfuly sent.
                        break;
                    }
                    catch (Exception err)
                    {
                        // The receiver is not available. Therefore try again if not timeout.
                    }

                    // If not timeout, then wait a wail and try again.
                    if (System.currentTimeMillis() - aStartSendingTime <= myMaxOfflineTime)
                    {
                        Thread.sleep(300);
                    }
                    else
                    {
                        // The timeout occured, therefore, clean the queue and stop sending.
                        synchronized (myEnqueuedMessages)
                        {
                            myEnqueuedMessages.clear();

                            myThreadIsSendingFlag = false;
                            return;
                        }
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IOutputChannel myUnderlyingOutputChannel;
    private ArrayList<Object> myEnqueuedMessages = new ArrayList<Object>();
    private boolean myThreadIsSendingFlag = false;
    private long myMaxOfflineTime;
    
    /*
    private String TracedObject()
    {
        return "Buffered output channel '" + getChannelId() + "' ";
    }
    */
}

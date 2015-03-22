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
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.threading.internal.*;

class ResponseMessageSender
{
    public ResponseMessageSender(String responseReceiverId, IDuplexInputChannel duplexInputChannel)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myResponseReceiverId = responseReceiverId;
            myDuplexInputChannel = duplexInputChannel;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void sendResponseMessage(Object message)
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
                    mySendingThreadShallStopFlag = false;
                    myThreadIsSendingFlag = true;
                    mySendingThreadStoppedEvent.reset();

                    ThreadPool.queueUserWorkItem(myMessageSenderHandler);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void stopSending()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myEnqueuedMessages)
            {
                // Indicate to the running thread, that the sending shall stop.
                mySendingThreadShallStopFlag = true;

                // Wait until the thread is stopped.
                try
                {
                    if (!mySendingThreadStoppedEvent.waitOne(5000))
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.FailedToStopThreadId);
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }

                // Remove all messages.
                myEnqueuedMessages.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void messageSender()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // Loop taking messages from the queue, until the queue is empty.
                while (!mySendingThreadShallStopFlag)
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
                            return;
                        }
                    }

                    while (!mySendingThreadShallStopFlag)
                    {
                        // Try to send the message.
                        try
                        {
                            // Send the message using the underlying output channel.
                            myDuplexInputChannel.sendResponseMessage(myResponseReceiverId, aMessage);

                            // The message was successfully sent, therefore it can be removed from the queue.
                            synchronized (myEnqueuedMessages)
                            {
                                myEnqueuedMessages.remove(0);
                            }

                            // The message was successfully sent.
                            break;
                        }
                        catch (Exception err)
                        {
                            // The receiver is not available. Therefore try again if not timeout.
                        }


                        // If sending thread is not asked to stop.
                        if (!mySendingThreadShallStopFlag)
                        {
                            Thread.sleep(300);
                        }
                    }
                }
            }
            finally
            {
                myThreadIsSendingFlag = false;
                mySendingThreadStoppedEvent.set();
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.DetectedException);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private String myResponseReceiverId;
    private ArrayList<Object> myEnqueuedMessages = new ArrayList<Object>();
    
    private boolean myThreadIsSendingFlag;
    private boolean mySendingThreadShallStopFlag;
    private ManualResetEvent mySendingThreadStoppedEvent = new ManualResetEvent(true);

    private IDuplexInputChannel myDuplexInputChannel;
    
    private Runnable myMessageSenderHandler = new Runnable()
    {
        @Override
        public void run()
        {
            messageSender();
        }
    };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " '" + myResponseReceiverId + "' ";
    }
}

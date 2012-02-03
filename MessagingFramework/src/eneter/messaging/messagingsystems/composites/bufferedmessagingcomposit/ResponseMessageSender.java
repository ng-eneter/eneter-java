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
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.threading.*;

class ResponseMessageSender
{
    public ResponseMessageSender(String responseReceiverId, IDuplexInputChannel duplexInputChannel, IMethod2<String, Boolean> lastActivityUpdater)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myResponseReceiverId = responseReceiverId;
            myDuplexInputChannel = duplexInputChannel;
            myLastActivityUpdater = lastActivityUpdater;
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

                    Runnable aSender = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                messageSender();
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
    
    public void stopSending() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myEnqueuedMessages)
            {
                // Indicate to the running thread, that the sending shall stop.
                mySendingThreadShallStopFlag = true;

                // Wait until the thread is stopped.
                if (!mySendingThreadStoppedEvent.waitOne(5000))
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.StopThreadFailure);
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
    
    private void messageSender() throws Exception
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

                            // Update the time for the response receiver.
                            // If the response receiver does not exist, then DO NOT create it.
                            // If the response receiver does not exist, it means there can be disconnecting.
                            myLastActivityUpdater.invoke(myResponseReceiverId, false);

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
    
    private IMethod2<String, Boolean> myLastActivityUpdater;
    
    private String TracedObject()
    {
        return "ResponseMessageSender '" + myResponseReceiverId + "' ";
    }
}

package eneter.messaging.messagingsystems.tcpmessagingsystem.internal;

import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ThreadPool;

/**
 * Helper class allowing to use timeout when sending messages via Socket.
 * Note: The reason while this class exists is that Java socket does not have a possibilities to setup the sending timeout. 
 *
 */
public class OutputStreamTimeoutWriter
{
    private class Worker implements Runnable
    {
        @Override
        public void run()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                try
                {
                    myOutputStream.write(myData, 0, myData.length);
                }
                catch (Exception err)
                {
                    myException = err;
                }
                finally
                {
                    mySendCompletedEvent.set();
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
    }
    
    
    public void write(OutputStream outputStream, byte[] data, int timeout)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized(myWorker)
            {
                // Prepare sending.
                myOutputStream = outputStream;
                myData = data;
                myException = null;
                mySendCompletedEvent.reset();
                
                // Start writing in another thread.
                ThreadPool.queueUserWorkItem(myWorker);
                
                // Wait until the writing is completed.
                if (!mySendCompletedEvent.waitOne(timeout))
                {
                    throw new TimeoutException("ResponseSender failed to send the message within specified timeout: " + Integer.toString(timeout) + "ms.");
                }
                
                if (myException != null)
                {
                    throw myException;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private Worker myWorker = new Worker();
    private OutputStream myOutputStream;
    private ManualResetEvent mySendCompletedEvent = new ManualResetEvent(false);
    private byte[] myData;
    private Exception myException;
}

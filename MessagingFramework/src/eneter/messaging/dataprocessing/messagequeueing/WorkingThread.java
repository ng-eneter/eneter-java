package eneter.messaging.dataprocessing.messagequeueing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eneter.messaging.diagnostic.*;
import eneter.net.system.IMethod1;

public class WorkingThread<_MessageType>
{
    public void RegisterMessageHandler(IMethod1<_MessageType> messageHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myHandlerLock)
            {
                if (myMessageHandler != null)
                {
                    String aMessage = TracedObject() + "has already registered the message handler.";
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
                
                myMessageHandler = messageHandler;
                
                // Start the single thread with the queue.
                myThreadPool = Executors.newSingleThreadExecutor();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void UnregisterMessageHandler()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myHandlerLock)
            {
                if (myThreadPool != null)
                {
                    try
                    {
                        myThreadPool.shutdownNow();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + " failed to stop the thread processing messages in the queue.", err);
                    }
                    catch (Error err)
                    {
                        EneterTrace.error(TracedObject() + " failed to stop the thread processing messages in the queue.", err);
                        throw err;
                    }
                    
                    myThreadPool = null;
                }
                
                myMessageHandler = null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void EnqueueMessage(final _MessageType message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myHandlerLock)
            {
                // Note: Only the execution of the following line and the instantiation of the
                //       anonymous class are synchronized.
                //       Methods in the anonymous class will be executed in a different thread
                //       and so they are not running the scope of the synchronized statement.
                myThreadPool.execute(new Runnable()
                {
                    // Store the reference for the case, some other thread unregisters it meanwhile.
                    IMethod1<_MessageType> aMyHandler = myMessageHandler;
                    
                    /**
                     * NOTE: This method will be executed later, by another thread.
                     *       Therefore, it will not run under this 'synchronize' scope!!!
                     *       -> and it is desired behavior.
                     */
                    @Override
                    public void run()
                    {
                        EneterTrace aTrace = EneterTrace.entering();
                        try
                        {
                            if (aMyHandler != null)
                            {
                                try
                                {
                                    aMyHandler.invoke(message);
                                }
                                catch (Exception err)
                                {
                                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                                }
                            }
                            else
                            {
                                EneterTrace.warning(TracedObject() + "processed message from the queue but the processing callback method was not registered.");
                            }
                        }
                        finally
                        {
                            EneterTrace.leaving(aTrace);
                        }
                    }
                });
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private ExecutorService myThreadPool;
    
    private IMethod1<_MessageType> myMessageHandler;
    
    private Object myHandlerLock = new Object();
    
    private String TracedObject()
    {
        return "The Working Thread ";
    }
}

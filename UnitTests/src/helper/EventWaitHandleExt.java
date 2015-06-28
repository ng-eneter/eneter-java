package helper;

import java.util.concurrent.TimeoutException;

import eneter.net.system.threading.internal.*;

public class EventWaitHandleExt
{
    public static void waitIfNotDebugging(ManualResetEvent waitHandle, int milliseconds) throws Exception
    {
        if (!Debugger.isAttached())
        {
            if (!waitHandle.waitOne(milliseconds))
            {
                throw new TimeoutException("Timeout " + milliseconds + " ms.");
            }
        }
        else
        {
            waitHandle.waitOne();
        }
    }
    
    public static void waitIfNotDebugging(AutoResetEvent waitHandle, int milliseconds) throws Exception
    {
        if (!Debugger.isAttached())
        {
            if (!waitHandle.waitOne(milliseconds))
            {
                throw new TimeoutException("Timeout " + milliseconds + " ms.");
            }
        }
        else
        {
            waitHandle.waitOne();
        }
    }
}

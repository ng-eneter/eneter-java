package eneter.messaging.diagnostic;

import static org.junit.Assert.*;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ThreadPool;
import helper.PerformanceTimer;

public class Test_ThreadLock
{
    @Test
    public void Performance() throws Exception
    {
        PerformanceTimer aTimer = new PerformanceTimer();
        
        final ManualResetEvent aCompleted = new ManualResetEvent(false);
        final Object aLock = new Object();
        final int[] aSum = {0};
        
        
        // Using synchronized keyword
        aTimer.start();
        
        for (int i = 0; i < 10; ++i)
        {
            ThreadPool.queueUserWorkItem(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int j = 0; j < 1000000; ++j)
                    {
                        synchronized (aLock)
                        {
                            try
                            {
                                ++aSum[0];
                                if (aSum[0] == 10000000)
                                {
                                    aCompleted.set();
                                }
                            }
                            catch (Exception e)
                            {
                            }
                        }
                    }
                }
            });
        }
        aCompleted.waitOne();
        aTimer.stop();

        // Using ReentrantLock
        final ReentrantLock aReentrantLock = new ReentrantLock();
        aCompleted.reset();
        aTimer.start();
        aSum[0] = 0;
        for (int i = 0; i < 10; ++i)
        {
            ThreadPool.queueUserWorkItem(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int j = 0; j < 1000000; ++j)
                    {
                        aReentrantLock.lock();
                        try
                        {
                            try
                            {
                                ++aSum[0];
                                if (aSum[0] == 10000000)
                                {
                                    aCompleted.set();
                                }
                            }
                            catch (Exception e)
                            {
                            }
                        }
                        finally
                        {
                            aReentrantLock.unlock();
                        }
                    }
                }
            });
        }
        aCompleted.waitOne();
        aTimer.stop();
        
        // Using ThreadLock
        final ThreadLock aThreadLock = new ThreadLock();
        aCompleted.reset();
        aTimer.start();
        aSum[0] = 0;
        for (int i = 0; i < 10; ++i)
        {
            ThreadPool.queueUserWorkItem(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int j = 0; j < 1000000; ++j)
                    {
                        aThreadLock.lock();
                        try
                        {
                            try
                            {
                                ++aSum[0];
                                if (aSum[0] == 10000000)
                                {
                                    aCompleted.set();
                                }
                            }
                            catch (Exception e)
                            {
                            }
                        }
                        finally
                        {
                            aThreadLock.unlock();
                        }
                    }
                }
            });
        }
        aCompleted.waitOne();
        aTimer.stop();
        
    }
}

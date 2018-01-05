/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.diagnostic;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.io.PrintStream;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ScalableThreadPool;


/**
 * Super duper trace.
 * 
 * Example showing how to enable tracing of communication errors and warnings to a file:
 * <pre>
 * EneterTrace.setDetailLevel(EneterTrace.EDetailLevel.Short);
 * EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
 * </pre>
 * 
 * Example showing how to enable tracing of detailed communication sequence to a file:
 * <pre>
 * EneterTrace.setDetailLevel(EneterTrace.EDetailLevel.Debug);
 * EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
 * </pre>
 * 
 * Example showing how you can trace entering/leaving methods:
 * <pre>
 *  class MyClass
 *  {
 *      ...
 *      <br/>
 *      void doSomething()
 *      {
 *          EneterTrace aTrace = EneterTrace.entering();
 *          try
 *          {
 *              ...
 *          }
 *          finally
 *          {
 *              EneterTrace.leaving(aTrace);
 *          }
 *      }
 *  }
 *  </pre>
 *  <br/>
 *  Output:
 *  <pre>
 *  {@code
 *  23:58:54.585 ~  1 --> some.namespace.MyClass.doSomething 
 *  23:58:54.585 ~  1 <-- some.namespace.MyClass.doSomething [0:0:0 0ms 5.0us]
 *  }
 *  </pre>
 *
 */
public class EneterTrace
{
    /**
     * 
     * Detail level of the trace.
     *
     */
    public enum EDetailLevel
    {
        /**
         *  Messages are not traced.
         */
        None,

        /** 
         * Info, Warning and Error messages.
         * The debug messages and entering - leaving messages are not traced.
         */
        Short,

        /**
         *  All messages are traced.
         */
        Debug
    }
    
    
    /**
     *  Traces entering-leaving the method.
     * 
     *  The entering information for the method calling this constructor is put to the trace
     *  and the measuring of the time starts.
     *  In order to trace entering-leaving, the detail level must be set to 'Debug'.
     *  
     */
    public static EneterTrace entering()
    {
        EneterTrace aTraceObject = null;

        if (myDetailLevel == EDetailLevel.Debug || myProfilerIsRunning)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            
            aTraceObject = new EneterTrace();
            aTraceObject.myCallStack = aStackTraceElements[2];
            
            long aEnteringTimeTicks = new Date().getTime();
            aTraceObject.myEnteringTicks = System.nanoTime();
           
            if (myProfilerIsRunning)
            {
                updateProfilerForEntering(aTraceObject);
            }
            else
            {
                writeMessage(aTraceObject.myCallStack, aEnteringTimeTicks, "-->", "", -1);
            }
        }
        
        return aTraceObject;
    }
    
    /**
     * Traces the leaving from the method.
     * @param trace The reference obtained during the entering the method.
     */
    public static void leaving(EneterTrace trace)
    {
        if (trace != null)
        {
            trace.leaving();
        }
    }
    
    
    /**
     * Traces the info message.
     * 
     * @param message info message
     */
    public static void info(String message)
    {
        if (myDetailLevel == EDetailLevel.None)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            if (aStackTraceElements.length >= 3)
            {
                long aTimeTicks = new Date().getTime();
                writeMessage(aStackTraceElements[2], aTimeTicks, " I:", message, -1);
            }
        }
    }
    
    
    /**
     * Traces the info message.
     * 
     * @param message info message
     * @param err exception that will be traced
     */
    public static void info(String message, Throwable err)
    {
        if (myDetailLevel == EDetailLevel.None)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            if (aStackTraceElements.length >= 3)
            {
                String aDetails = getDetailsFromException(err);
                long aTimeTicks = new Date().getTime();
                writeMessage(aStackTraceElements[2], aTimeTicks, " I:", message + "\r\n" + aDetails, -1);
            }
        }
    }

    
    /**
     * Traces the warning message.
     * 
     * @param message warning message
     */
    public static void warning(String message)
    {
        if (myDetailLevel == EDetailLevel.None)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            if (aStackTraceElements.length >= 3)
            {
                long aTimeTicks = new Date().getTime();
                writeMessage(aStackTraceElements[2], aTimeTicks, " W:", message, -1);
            }
        }
    }
    
    /**
     * Traces the warning message. (internal eneter method)
     * 
     * @param callstackIndex
     * @param message warning message
     */
    public static void warning(int callstackIndex, String message)
    {
        if (myDetailLevel == EDetailLevel.None)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            if (aStackTraceElements.length >= 3 + callstackIndex)
            {
                long aTimeTicks = new Date().getTime();
                writeMessage(aStackTraceElements[2 + callstackIndex], aTimeTicks, " W:", message, -1);
            }
        }
    }
    
    
    /**
     * Traces the warning message.
     * 
     * @param message warning message
     * @param err exception that will be traced
     */
    public static void warning(String message, Throwable err)
    {
        if (myDetailLevel == EDetailLevel.None)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            if (aStackTraceElements.length >= 3)
            {
                String aDetails = getDetailsFromException(err);
                long aTimeTicks = new Date().getTime();
                writeMessage(aStackTraceElements[2], aTimeTicks, " W:", message + "\r\n" + aDetails, -1);
            }
        }
    }

    
    /**
     * Traces the error message.
     * 
     * @param message error message
     */
    public static void error(String message)
    {
        if (myDetailLevel == EDetailLevel.None)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            if (aStackTraceElements.length >= 3)
            {
                long aTimeTicks = new Date().getTime();
                writeMessage(aStackTraceElements[2], aTimeTicks, " E:", message, -1);
            }
        }
    }

    /**
     * Traces the error message.
     * 
     * @param message error message
     * @param err exception that will be traced
     */
    public static void error(String message, Throwable err)
    {
        if (myDetailLevel == EDetailLevel.None)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            if (aStackTraceElements.length >= 3)
            {
                String aDetails = getDetailsFromException(err);
                long aTimeTicks = new Date().getTime();
                writeMessage(aStackTraceElements[2], aTimeTicks, " E:", message + "\r\n" + aDetails, -1);
            }
        }
    }

    
    /**
     * Traces the warning message. (internal eneter method)
     * 
     * @param callstackIndex
     * @param message warning message
     */
    public static void debug(int callstackIndex, String message)
    {
        if (myDetailLevel == EDetailLevel.Debug)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            if (aStackTraceElements.length >= 3 + callstackIndex)
            {
                long aTimeTicks = new Date().getTime();
                writeMessage(aStackTraceElements[2 + callstackIndex], aTimeTicks, " D:", message, -1);
            }
        }
    }
    

    /**
     * Traces the debug message.
     * 
     * To trace debug messages, the detail level must be set to debug.
     * 
     * @param message debug message
     */
    public static void debug(String message)
    {
        if (myDetailLevel == EDetailLevel.Debug)
        {
            StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
            if (aStackTraceElements.length >= 3)
            {
                long aTimeTicks = new Date().getTime();
                writeMessage(aStackTraceElements[2], aTimeTicks, " D:", message, -1);
            }
        }
    }
    
    /**
     * Starts the profiler measurement.
     */
    public static void startProfiler()
    {
        synchronized (myProfilerData)
        {
            writeToTrace("Profiler is running...\r\n");
            myProfilerIsRunning = true;
        }
    }
    
    /**
     * Stops the profiler measurement and writes results to the trace.
     */
    public static void stopProfiler()
    {
        // Wait until all items are processed.
        try
        {
            myQueueThreadEndedEvent.waitOne();
            
            synchronized (myProfilerData)
            {
                myProfilerIsRunning = false;

                Collection<Entry<String, ProfilerData>> aProfilerDataCollection = myProfilerData.entrySet();
                List<Entry<String, ProfilerData>> aOrderedProfilerData = new ArrayList<Entry<String, ProfilerData>>(aProfilerDataCollection);
                Collections.sort(aOrderedProfilerData, new Comparator<Entry<String, ProfilerData>>()
                    {
                        // Order descending
                        @Override
                        public int compare(Entry<String, ProfilerData> o1, Entry<String, ProfilerData> o2)
                        {
                            if (o1.getValue().Ticks < o2.getValue().Ticks)
                            {
                                return 1;
                            }
                            
                            if (o1.getValue().Ticks > o2.getValue().Ticks)
                            {
                                return -1;
                            }
                            
                            return 0;
                        }
                    });
                
                for (Entry<String, ProfilerData> anItem : aOrderedProfilerData)
                {
                    String aElapsedTime = nanoSecondsToTimeStamp(anItem.getValue().Ticks);
                    String aTimePerCall = nanoSecondsToTimeStamp((long)Math.round(((double)anItem.getValue().Ticks) / anItem.getValue().Calls));

                    StringBuilder aMessageBuilder = new StringBuilder()
                            .append(aElapsedTime).append(" ").append(anItem.getValue().Calls).append("x |").append(anItem.getValue().MaxConcurency).append("| #").append(anItem.getValue().MaxRecursion).append(" ").append(aTimePerCall).append(" ").append(anItem.getKey()).append("\r\n");
                    String aMessage = aMessageBuilder.toString();

                    writeToTrace(aMessage);
                }

                myProfilerData.clear();

                writeToTrace("Profiler has ended.\r\n");
            }
        }
        catch (InterruptedException e)
        {
        }
    }
    
    
    /**
     * Gets the user defined trace.
     * 
     * If the value is set, the trace messages are written to the specified trace.
     * If the value is null, then messages are written only to the debug port.
     */
    public static PrintStream getTraceLog()
    {
        synchronized (myTraceLogLock)
        {
            return myTraceLog;
        }
    }
    

    /**
     * Sets the user defined trace.
     * 
     * If the value is set, the trace messages are written to the specified trace.
     * If the value is null, then messages are written only to the debug port.
     */
    public static void setTraceLog(PrintStream value)
    {
        synchronized (myTraceLogLock)
        {
            myTraceLog = value;
        }
    }
    
    /**
     * Gets the detail level of the trace.
     * 
     * @return EDetailLevel enumerator
     */
    public static EDetailLevel getDetailLevel()
    { 
        return myDetailLevel;
    }
    
    
    /**
     * Sets the detail level of the trace.
     * 
     * If the detail level is set to 'Short' then only info, warning and error messages are traced.<br/>
     * If the detail level is set to 'Debug' then all messages are traced.
     * 
     * @param value
     */
    public static void setDetailLevel(EDetailLevel value)
    {
        myDetailLevel = value;
    }
    
    
    /**
     *  Gets the regular expression that will be applied to the namespace to filter traced messages.
     */
    public static Pattern getNameSpaceFilter()
    {
        synchronized (myTraceLogLock)
        {
            return myNameSpaceFilter;
        }
    }
    
    
    /**
     * Sets or gets the regular expression that will be applied to the namespace to filter traced messages.
     *
     *  @param value
     *  
     * If the namespace matches with the regular expression, the message will be traced.
     * If the filter is set to null, then the filter is not used and all messages will be traced.<br/>
     * <br/>
     * The following example shows how to set the filter to trace a certain namespace.
     * <pre>
     * {@code
     * // Set the debug detailed level.
     * EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
     *
     * // Examples:
     * // Traces all name spaces starting with 'My.NameSpace'.
     * EneterTrace.NameSpaceFilter = Pattern.compile("^My\.NameSpace");
     *
     * // Traces exactly the name space 'My.NameSpace'.
     * EneterTrace.NameSpaceFilter = Pattern.compile("^My\.NameSpace$");
     *
     * // Traces name spaces starting with 'Calc.Methods' or 'App.Utilities'.
     * EneterTrace.NameSpaceFilter = Pattern.compile("^Calc\.Methods|^App\.Utilities");
     *
     * // Traces all name spaces except namespaces starting with 'Eneter'.
     * EneterTrace.NameSpaceFilter = Pattern.compile("^(?!\bEneter\b)");
     * }
     * </pre>
     */
    public static void setNameSpaceFilter(Pattern value)
    {
        synchronized (myTraceLogLock)
        {
            myNameSpaceFilter = value;
        }
    }
    
    
    private static String getDetailsFromException(Throwable err)
    {
        // If there is not exception, then return empty string.
        if (err == null)
        {
            return "";
        }
        

        // Get the exception details.
        StringBuilder aDetails = new StringBuilder();
        aDetails.append(String.format("Exception:\r\n%s: %s\r\n%s", err.getClass().getName(), err.getMessage(), getStackTraceString(err.getStackTrace())));
        
        // Get all inner exceptions.
        Throwable anInnerException = err.getCause();
        while (anInnerException != null)
        {
            aDetails.append(String.format("\r\n\r\n%s: %s\r\n%s", anInnerException.getClass().getName(), anInnerException.getMessage(), getStackTraceString(anInnerException.getStackTrace())));

            // Get the next inner exception.
            anInnerException = anInnerException.getCause();
        }

        aDetails.append("\r\n==========\r\n");

        return aDetails.toString();
    }
    
    private static String getStackTraceString(StackTraceElement[] stackTrace)
    {
        StringBuilder aResult = new StringBuilder();
        for (int i = 0; i < stackTrace.length; ++i)
        {
            aResult.append(stackTrace[i].toString());
            
            // If it is not the last element then add the next line.
            if (i < stackTrace.length - 1)
            {
                aResult.append("\r\n");
            }
        }
        
        return aResult.toString();
    }
    
    
    private static void writeMessage(final StackTraceElement stack, final long milliTicks, final String prefix, final String message, final long elapsedNanoTicks)
    {
        final long aThreadId = Thread.currentThread().getId();
        
        
        // Composes trace message and writes it to the buffer.
        Runnable aDoWrite = new Runnable()
        {
            @Override
            public void run()
            {
                String aMethodName = stack.getClassName() + "." + stack.getMethodName();   
                
                // Check the filter.
                if (myNameSpaceFilter != null && !myNameSpaceFilter.matcher(aMethodName).matches())
                {
                    return;
                }
                
                String aTimeStr = milliTicks > -1 ? milliSecondsToTimeStamp(milliTicks) : null;
                String aElapsedTicksStr = (elapsedNanoTicks > -1) ? nanoSecondsToTimeStamp(elapsedNanoTicks) : null;
                
                synchronized (myTraceBufferLock)
                {
                    boolean aStartTimerFlag = myTraceBuffer.length() == 0;
                    
                    myTraceBuffer
                    .append(aTimeStr)
                    .append(" ~")
                    .append(aThreadId).append(" ")
                    .append(prefix).append(" ")
                    .append(aMethodName);
                    
                    if (aElapsedTicksStr != null)
                    {
                        myTraceBuffer.append(" [").append(aElapsedTicksStr).append("]\r\n");
                    }
                    else
                    {
                        myTraceBuffer.append(" ").append(message).append("\r\n");
                    }
                    
                    

                    if (aStartTimerFlag)
                    {
                        // Flush the buffer in the specified time.
                        myTraceBufferFlushTimer.schedule(getTimerTask(), 100);
                    }
                }
            }
        };

        myWritingThread.execute(aDoWrite);
    }
    
    private static void updateProfilerForEntering(final EneterTrace trace)
    {
        final long aThreadId = Thread.currentThread().getId();

        Runnable aProfilerJob = new Runnable()
        {
            @Override
            public void run()
            {
                String aMethod = trace.myCallStack.getClassName() + "." + trace.myCallStack.getMethodName();

                synchronized (myProfilerData)
                {
                    ProfilerData aProfileData = myProfilerData.get(aMethod);
                    if (aProfileData == null)
                    {
                        aProfileData = new ProfilerData();
                        aProfileData.Calls = 1;
                        aProfileData.MaxConcurency = 1;
                        aProfileData.MaxRecursion = 1;
                        
                        MutableInt aIntValue = new MutableInt();
                        aIntValue.Value = 1;
                                
                        aProfileData.Threads.put(aThreadId, aIntValue);

                        myProfilerData.put(aMethod, aProfileData);
                    }
                    else
                    {
                        ++aProfileData.Calls;

                        // If this thread is already inside then it is a recursion.
                        if (aProfileData.Threads.containsKey(aThreadId))
                        {
                            int aRecursion = ++aProfileData.Threads.get(aThreadId).Value;
                            if (aRecursion > aProfileData.MaxRecursion)
                            {
                                aProfileData.MaxRecursion = aRecursion;
                            }
                        }
                        // ... else it is another thread which is parallel inside.
                        else
                        {
                            MutableInt aIntValue = new MutableInt();
                            aIntValue.Value = 1;
                            
                            aProfileData.Threads.put(aThreadId, aIntValue);
                            if (aProfileData.Threads.size() > aProfileData.MaxConcurency)
                            {
                                aProfileData.MaxConcurency = aProfileData.Threads.size();
                            }
                        }
                    }

                    trace.myBufferedProfileData = aProfileData;
                }
            }
        };
        
        myWritingThread.execute(aProfilerJob);
    }
    
    private static void updateProfilerForLeaving(final EneterTrace trace, final long ticks)
    {
        final long aThreadId = Thread.currentThread().getId();

        Runnable aProfilerJob = new Runnable()
        {
            @Override
            public void run()
            {
                synchronized (myProfilerData)
                {
                    trace.myBufferedProfileData.Ticks += ticks;
                    int aRecursion = --trace.myBufferedProfileData.Threads.get(aThreadId).Value;
                    
                    if (aRecursion < 1)
                    {
                        String aMethod = trace.myCallStack.getClassName() + "." + trace.myCallStack.getMethodName();
                        ProfilerData aProfileData = myProfilerData.get(aMethod);
                        aProfileData.Threads.remove(aThreadId);
                    }
                }
            }
        };
        
        myWritingThread.execute(aProfilerJob);
    }
    
    private static String milliSecondsToTimeStamp(long milliTicks)
    {
        Date aDateTime = new Date(milliTicks);
        String aResult = myDateFormatter.format(aDateTime);
        return aResult;
    }
    
    private static String nanoSecondsToTimeStamp(long elapsedNanoTicks)
    {
        long aHours = (long) (elapsedNanoTicks / (60.0 * 60.0 * 1000000000.0));
        elapsedNanoTicks -= aHours * 60 * 60 * 1000000000;
        
        long aMinutes = (long) (elapsedNanoTicks / (60.0 * 1000000000.0));
        elapsedNanoTicks -= aMinutes * 60 * 1000000000;
        
        double aSeconds = elapsedNanoTicks / 1000000000.0;
        
        String aSecondsStr = new DecimalFormat("0.000000").format(aSeconds);
        
        StringBuilder aTimeStampBuilder = new StringBuilder()
                .append(aHours).append(":").append(aMinutes).append(":").append(aSecondsStr);
        
        String aResult = aTimeStampBuilder.toString();
        return aResult;
    }
    
    // Invoked by timer.
    private static void onFlushTraceBufferTick()
    {
        StringBuilder aNewBuffer = new StringBuilder(myTraceBufferCapacity);
        StringBuilder aBufferToFlush;

        // Keep the lock for the shortest possible time.
        synchronized (myTraceBufferLock)
        {
            aBufferToFlush = myTraceBuffer;
            myTraceBuffer = aNewBuffer;
        }

        String aBufferedTraceMessages = aBufferToFlush.toString();

        // Flush buffered messages to the trace.
        writeToTrace(aBufferedTraceMessages);
    }
    
    /*
     * Helper method to get the new instance of the timer task.
     * The problem is, the timer does not allow to reschedule the same instance of the TimerTask
     * and the exception is thrown.
     */
    private static TimerTask getTimerTask()
    {
        TimerTask aTimerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                onFlushTraceBufferTick();
            }
        };
        
        return aTimerTask;
    }

    
    private static void writeToTrace(String message)
    {
        try
        {
            synchronized (myTraceLogLock)
            {
                // If a trace log is set, then use it.
                if (myTraceLog != null)
                {
                    myTraceLog.print(message);
                    myTraceLog.flush();
                }
                else
                {
                    // Otherwise write to the default output stream
                    System.out.print(message);
                }
            }
        }
        catch (Exception err)
        {
            String anExceptionDetails = getDetailsFromException(err);
            System.out.println("EneterTrace failed to write to the trace." + anExceptionDetails);
        }
    }
    
    /**
     * Private helper constructor.
     * The constructor is private, so the class can be instantiating only via the 'Entering' method.
     */
    private EneterTrace()
    {
    }

    /**
     * Traces the leaving from a method.
     */
    private void leaving()
    {
        try
        {
            if (myEnteringTicks != 0)
            {
                long aLeavingTicks = System.nanoTime();
                long aLeavingTimeTicks = new Date().getTime();
                
                long aElapsedTicks = aLeavingTicks - myEnteringTicks;

                if (myProfilerIsRunning)
                {
                    updateProfilerForLeaving(this, aElapsedTicks);
                }
                else if (myDetailLevel == EDetailLevel.Debug)
                {
                    writeMessage(myCallStack, aLeavingTimeTicks, "<--", null, aElapsedTicks);
                }
            }
        }
        catch (Exception err)
        {
            // Any exception in this Dispose method is irrelevant.
        }
    }
    
    
    private long myEnteringTicks = Long.MIN_VALUE;
    private StackTraceElement myCallStack;
    private ProfilerData myBufferedProfileData;
    
    
    // Trace Info, Warning and Error by default.
    private static EDetailLevel myDetailLevel = EDetailLevel.Short;
    private static Object myTraceLogLock = new Object();
    private static PrintStream myTraceLog;
    private static Pattern myNameSpaceFilter;
    
    private static SimpleDateFormat myDateFormatter = new SimpleDateFormat("HH:mm:ss.SSSSSS");
    
    private static ManualResetEvent myQueueThreadEndedEvent = new ManualResetEvent(true);
    private static Object myTraceBufferLock = new Object();
    private static int myTraceBufferCapacity = 16384;
    private static StringBuilder myTraceBuffer = new StringBuilder(myTraceBufferCapacity);
    private static Timer myTraceBufferFlushTimer = new Timer("Eneter.TraceFlushTimer", true);
    
    private static class MutableInt
    {
        public int Value;
    }
    
    private static class ProfilerData
    {
        public long Calls;
        public long Ticks;
        public int MaxConcurency;
        public int MaxRecursion;

        public HashMap<Long, MutableInt> Threads = new HashMap<Long, MutableInt>();
    }
    
    private static HashMap<String, ProfilerData> myProfilerData = new HashMap<String, ProfilerData>();
    private static volatile boolean myProfilerIsRunning;
    
    // Ensures sequential writing of messages.
    //private static ExecutorService myWritingThread = Executors.newSingleThreadExecutor(new ThreadFactory()
    private static ScalableThreadPool myWritingThread = new ScalableThreadPool(0, 1, 5000, new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            Thread aNewThread = new Thread(r, "Eneter.TraceWriter");
            
            // Thread shall not block the application to shutdown.
            aNewThread.setDaemon(true);
            return aNewThread;
        }
    });
}
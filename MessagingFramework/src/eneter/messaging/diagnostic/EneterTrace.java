/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.diagnostic;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.io.PrintStream;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

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

        if (myDetailLevel == EDetailLevel.Debug)
        {
            aTraceObject = new EneterTrace();

            writeMessage("-->", "", 3);

            aTraceObject.myEnteringTime = System.nanoTime();
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
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" I:", message, 3);
        }
    }
    
    
    /**
     * Traces the information message and details.
     * 
     * @param message info message
     * @param details additional details
     */
    public static void info(String message, String details)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" I:", message + "\r\nDetails: " + details, 3);
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
        if (myDetailLevel != EDetailLevel.None)
        {
            String aDetails = getDetailsFromException(err);
            writeMessage(" I:", message + "\r\n" + aDetails, 3);
        }
    }

    
    /**
     * Traces the warning message.
     * 
     * @param message warning message
     */
    public static void warning(String message)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" W:", message, 3);
        }
    }

    
    /**
     * Traces the warning message and details
     * 
     * @param message warning message
     * @param details additional details
     */
    public static void warning(String message, String details)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" W:", message + "\r\nDetails: " + details, 3);
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
        if (myDetailLevel != EDetailLevel.None)
        {
            String aDetails = getDetailsFromException(err);
            writeMessage(" W:", message + "\r\n" + aDetails, 3);
        }
    }

    
    /**
     * Traces the error message.
     * 
     * @param message error message
     */
    public static void error(String message)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" E:", message, 3);
        }
    }


    /**
     * Traces the error message and details for the error.
     * 
     * @param message error message
     * @param errorDetails additional details
     */
    public static void error(String message, String errorDetails)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" E:", message + "\r\nDetails: " + errorDetails, 3);
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
        if (myDetailLevel != EDetailLevel.None)
        {
            String aDetails = getDetailsFromException(err);
            writeMessage(" E:", message + "\r\n" + aDetails, 3);
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
            writeMessage(" D:", message, 3);
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
    
    
    private static void writeMessage(final String prefix, final String message, final int callStackIdx)
    {
        final Date aDate = new Date();
        final StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
        final long aThreadId = Thread.currentThread().getId();
        
        
        // Composes trace message and writes it to the buffer.
        Runnable aDoWrite = new Runnable()
        {
            @Override
            public void run()
            {
                StackTraceElement aCaller = aStackTraceElements[callStackIdx];
                String aMethodName = aCaller.getClassName() + "." + aCaller.getMethodName();        
                
                
                
                // Check if the message matches with the filter.
                // Note: If the filter is not set or string matches.
                if (myNameSpaceFilter == null || myNameSpaceFilter.matcher(aMethodName).matches())
                {
                    synchronized (myTraceBufferLock)
                    {
                        boolean aStartTimerFlag = myTraceBuffer.length() == 0;
                        
                        myTraceBuffer
                        .append(myDateFormatter.format(aDate))
                        .append(" ~")
                        .append(aThreadId)
                        .append(" ")
                        .append(prefix)
                        .append(" ")
                        .append(aMethodName)
                        .append(" ")
                        .append(message)
                        .append("\r\n");

                        if (aStartTimerFlag)
                        {
                            // The buffer will be flushed to the trace in 50 ms. 
                            myTraceBufferFlushTimer.schedule(getTimerTask(), 50);
                        }
                    }
                }
            }
        };

        myWritingThread.execute(aDoWrite);
    }
    
    
    // Invoked by timer.
    private static void onFlushTraceBufferTick()
    {
        String aBufferedTraceMessages;

        synchronized (myTraceBufferLock)
        {
            aBufferedTraceMessages = myTraceBuffer.toString();
            
            if (myTraceBuffer.capacity() <= myTraceBufferCapacity)
            {
                myTraceBuffer.setLength(0);
            }
            else
            {
                // The allocated capacity is too big. So instantiate the new buffer and the old one can be garbage collected.
                myTraceBuffer = new StringBuilder();
            }
        }

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
            if (myEnteringTime != Long.MIN_VALUE)
            {
                long anElapsedTime = System.nanoTime() - myEnteringTime;
                
                long aHours = (long) (anElapsedTime / (60.0 * 60.0 * 1000000000.0));
                anElapsedTime -= aHours * 60 * 60 * 1000000000;
                
                long aMinutes = (long) (anElapsedTime / (60.0 * 1000000000.0));
                anElapsedTime -= aMinutes * 60 * 1000000000;
                
                long aSeconds = anElapsedTime / 1000000000;
                anElapsedTime -= aSeconds * 1000000000;
                
                long aMiliseconds = anElapsedTime / 1000000;
                anElapsedTime -= aMiliseconds * 1000000;
                
                // Microseconds rounded to one digit place.
                double aMicroseconds = Math.round(anElapsedTime / 100.0) / 10.0;

                StringBuilder aMessage = new StringBuilder()
                .append("[").append(aHours).append(":").append(aMinutes).append(":").append(aSeconds).append(" ")
                .append(aMiliseconds).append("ms ")
                .append(aMicroseconds).append("us]");
                
                writeMessage("<--", aMessage.toString(), 4);
            }
        }
        catch(Exception exception)
        {
            // Any exception in this Dispose method is irrelevant.
        }
    }
    
    
    private long myEnteringTime = Long.MIN_VALUE;
    
    
    // Trace Info, Warning and Error by default.
    private static EDetailLevel myDetailLevel = EDetailLevel.Short;
    private static Object myTraceLogLock = new Object();
    private static PrintStream myTraceLog;
    private static Pattern myNameSpaceFilter;
    
    private static SimpleDateFormat myDateFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
    
    private static Object myTraceBufferLock = new Object();
    private static int myTraceBufferCapacity = 16384;
    private static StringBuilder myTraceBuffer = new StringBuilder(myTraceBufferCapacity);
    private static Timer myTraceBufferFlushTimer = new Timer("Eneter.TraceFlushTimer", true);
    
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
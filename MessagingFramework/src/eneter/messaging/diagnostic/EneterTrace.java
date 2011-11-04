/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright � 2012 Martin Valach and Ondrej Uzovic
 * 
 */
package eneter.messaging.diagnostic;

import java.util.Date;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import eneter.net.system.threading.ThreadPool;

public class EneterTrace implements AutoCloseable
{
    /**
     * 
     * Detail level of the trace.
     *
     */
    public enum EDetailLevel
    {
        // Messages are not traced.
        None,

        // Info, Warning and Error messages.<br/>
        // The debug messages and enetering - leaving messages are not traced.
        Short,

        // All messages.
        Debug
    }
    
    
    /**
     *  Traces entering-leaving the method.
     * 
     *  The enetering information for the method calling this constructor is put to the trace
     *  and the measuring of the time starts.
     *  In order to trace entering-leaving, the detail level must be set to 'Debug'.
     *  
     *  @return
     * 
     */
    public static AutoCloseable entering()
    {
        EneterTrace aTraceObject = null;

        if (myDetailLevel == EDetailLevel.Debug)
        {
            aTraceObject = new EneterTrace();

            writeMessage("-->", null);

            aTraceObject.myEnteringTime = System.currentTimeMillis();
        }

        return aTraceObject;
    }
    
    
    /**
     * Traces the leaving from the method including the duration time.
     * 
     * (non-Javadoc)
     * @see eneter.messaging.diagnostic.AutoCloseable#close()
     */
    @Override
    public void close()
    {
        try
        {
            if (myEnteringTime != Long.MIN_VALUE)
            {
                long aDurationMillis = System.currentTimeMillis() - myEnteringTime;
                
                long aDays = TimeUnit.MILLISECONDS.toDays(aDurationMillis);
                aDurationMillis -= TimeUnit.DAYS.toMillis(aDays);
                long aHours = TimeUnit.MILLISECONDS.toHours(aDurationMillis);
                aDurationMillis -= TimeUnit.HOURS.toMillis(aHours);
                long aMinutes = TimeUnit.MILLISECONDS.toMinutes(aDurationMillis);
                aDurationMillis -= TimeUnit.MINUTES.toMillis(aMinutes);
                long aSeconds = TimeUnit.MILLISECONDS.toSeconds(aDurationMillis);

                writeMessage("<--", String.format("[%1$2d:%2$2d:%3$2d.%4$3dms]",
                    aHours,
                    aMinutes,
                    aSeconds,
                    aDurationMillis));
            }
        }
        catch(Exception exception)
        {
            // Any exception in this Dispose method is irrelevant.
        }
    }
    
    
    /**
     * Traces the info message.
     * 
     * @param message
     */
    public static void info(String message)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" I:", message);
        }
    }
    
    
    /**
     * Traces the information message and details
     * 
     * @param message
     * @param details
     */
    public static void info(String message, String details)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" I:", message + "\r\nDetails: " + details);
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
            writeMessage(" I:", message + "\r\n" + aDetails);
        }
    }

    
    /**
     * Traces the warning message.
     * 
     * @param message
     */
    public static void warning(String message)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" W:", message);
        }
    }

    
    /**
     * Traces the warning message and details
     * 
     * @param message
     * @param details
     */
    public static void warning(String message, String details)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" W:", message + "\r\nDetails: " + details);
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
            writeMessage(" W:", message + "\r\n" + aDetails);
        }
    }

    
    /**
     * Traces the error message.
     * 
     * @param message
     */
    public static void error(String message)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" E:", message);
        }
    }


    /**
     * Traces the error message and details for the error.
     * 
     * @param message
     * @param errorDetails
     */
    public static void error(String message, String errorDetails)
    {
        if (myDetailLevel != EDetailLevel.None)
        {
            writeMessage(" E:", message + "\r\nDetails: " + errorDetails);
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
            writeMessage(" E:", message + "\r\n" + aDetails);
        }
    }


    /**
     * Traces the debug message.
     * 
     * To trace debug messages, the detail level must be set to debug.
     * 
     * @param message
     */
    public static void debug(String message)
    {
        if (myDetailLevel == EDetailLevel.Debug)
        {
            writeMessage(" D:", message);
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
     * If the filter is set to null, then the filter is not used and all messages will be traced.
     * <example>
     * The following example shows how to set the filter to trace a certain namespace.
     * <code>
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
     * </code>
     * </example>
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
        
        return err.toString();

        /*
        // Get the exception details.
        StringBuilder aDetails = new StringBuilder();
        aDetails.AppendFormat(CultureInfo.InvariantCulture, "Exception:\r\n{0}: {1}\r\n{2}", err.GetType(), err.Message, err.StackTrace);

        // Get all inner exceptions.
        Exception anInnerException = err.InnerException;
        while (anInnerException != null)
        {
            aDetails.AppendFormat(CultureInfo.InvariantCulture, "\r\n\r\n{0}: {1}\r\n{2}", anInnerException.GetType(), anInnerException.Message, anInnerException.StackTrace);

            // Get the next inner exception.
            anInnerException = anInnerException.InnerException;
        }

        aDetails.Append("\r\n==========\r\n");

        return aDetails.ToString();
        */
    }
    
    
    private static void writeMessage(String prefix, String message)
    {
        //get current date time with Date()
        Date aDate = new Date();

        // Get the calling method
        StackTraceElement[] aStackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement aCaller = aStackTraceElements[3];
        final String aMethodName = aCaller.getClassName() + "." + aCaller.getMethodName();        
        final String aMessage = String.format("%1$tH:%1$tM:%1$tS.%1$tL ~%2$3d %3$s %4$s %5$s",
            aDate,
            Thread.currentThread().getId(),
            prefix, aMethodName, message);
        
        // anonymous instance as a variable
        Runnable aDoWrite = new Runnable()
        {
            @Override
            public void run()
            {
                synchronized(myTraceLogLock)
                {
                    // Check if the message matches with the filter.
                    // Note: If the filter is not set or string matches.
                    if (myNameSpaceFilter == null || myNameSpaceFilter.matcher(aMethodName).matches())
                    {
                        // If a trace log is set, then write to it.
                        if (myTraceLog != null)
                        {
                            myTraceLog.println(aMessage);
                            myTraceLog.flush();
                        }
                        else
                        {
                            // Otherwise write to the default error stream
                            System.err.println(aMessage);
                        }
                    }
                }
            }
        };

        ThreadPool.queueUserWorkItem(aDoWrite);
    }
    
    
    /// <summary>
    /// Private helper constructor.
    /// </summary>
    /// <remarks>
    /// The constructor is private, so the class can be instantiating only via the 'Entering' method.
    /// </remarks>
    private EneterTrace()
    {
    }
    
    
    private long myEnteringTime = Long.MIN_VALUE;
    
    // Trace Info, Warning and Error by default.
    private static EDetailLevel myDetailLevel = EDetailLevel.Short;

    private static Object myTraceLogLock = new Object();
    
    private static PrintStream myTraceLog;
    
    private static Pattern myNameSpaceFilter;
}
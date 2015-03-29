package helper;


public class Debugger
{
    public static boolean isAttached()
    {
        return myIsAttached;
    }
    
    private static boolean myIsAttached = java.lang.management.ManagementFactory.getRuntimeMXBean()
            .getInputArguments().toString().indexOf("jdwp") >= 0;
}

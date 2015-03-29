package helper;

public class PerformanceTimer
{
    public void start()
    {
        myStartingTime = System.nanoTime();
    }

    public void stop()
    {
        long anElapsedTime = System.nanoTime() - myStartingTime;
        
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
        
        System.out.println("Elapsed time = " + aMessage);
    }

    private long myStartingTime;
}

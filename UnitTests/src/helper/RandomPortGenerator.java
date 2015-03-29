package helper;

import java.util.Random;

public class RandomPortGenerator
{
    public static String generate()
    {
        return Integer.toString(generateInt());
    }
    
    public static int generateInt()
    {
        Random aRnd = new Random();
        int aPort = 7000 + aRnd.nextInt(1000);
        return aPort;
    }
}

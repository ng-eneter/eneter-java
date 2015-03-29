package helper;

import java.util.Random;

public class RandomDataGenerator
{
    public static String getString(int length)
    {
        StringBuilder aStringBuilder = new StringBuilder();

        for (int i = 0; i < length; ++i)
        {
            char ch = (char)((int)(Math.random() * 26 + 97)); 
            aStringBuilder.append(ch);
        }

        return aStringBuilder.toString();
    }
    
    public static byte[] getBytes(int length)
    {
        byte[] aResult = new byte[length];

        Random aRnd = new Random();
        aRnd.nextBytes(aResult);

        return aResult;
    }
}

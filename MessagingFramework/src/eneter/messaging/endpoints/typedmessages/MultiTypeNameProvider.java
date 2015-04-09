/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;


class MultiTypeNameProvider
{
    public static String getNetName(Class<?> clazz)
    {
        String aNetName;
        
        // Correct the name according to .NET
        if (clazz == Character.class)
        {
            aNetName = "Char";
        }
        else if (clazz == Float.class)
        {
            aNetName = "Single";
        }
        else if (clazz == Short.class)
        {
            aNetName = "Int16";
        }
        else if (clazz == Integer.class)
        {
            aNetName = "Int32";
        }
        else if (clazz == Long.class)
        {
            aNetName = "Int64";
        }
        else if (clazz.isArray())
        {
            if (clazz == byte[].class || clazz == Byte[].class)
            {
                aNetName = "Byte[]";
            }
            else if (clazz == char[].class || clazz == Character[].class)
            {
                aNetName = "Char[]";
            }
            else if (clazz == float[].class || clazz == Float[].class)
            {
                aNetName = "Single[]";
            }
            else if (clazz == double[].class || clazz == Double[].class)
            {
                aNetName = "Double[]";
            }
            else if (clazz == short[].class || clazz == Short[].class)
            {
                aNetName = "Int16[]";
            }
            else if (clazz == Integer[].class || clazz == int[].class)
            {
                aNetName = "Int32[]";
            }
            else if (clazz == Long[].class || clazz == long[].class)
            {
                aNetName = "Int64[]";
            }
            else
            {
                aNetName = clazz.getSimpleName();
            }
        }
        else
        {
            aNetName = clazz.getSimpleName();
        }
        
        return aNetName;
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.collections.generic.internal;

import java.util.*;

import eneter.net.system.IFunction1;

public class HashSetExt
{
    public static <T> int removeWhere(HashSet<T> hashSet, IFunction1<Boolean, T> match)
        throws Exception
    {
        int aNumberOfRemoved = 0;
        
        for (Iterator<T> i = hashSet.iterator(); i.hasNext();)
        {
            T anItem = i.next();
            
            if (match.invoke(anItem).booleanValue())
            {
                ++aNumberOfRemoved;
                i.remove();
            }
        }
        
        return aNumberOfRemoved;
    }
}
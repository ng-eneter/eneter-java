/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.linq.internal;

import java.util.ArrayList;
import java.util.Iterator;

import eneter.net.system.internal.IFunction1;

public class EnumerableExt
{
    public static <T> Iterable<T> where(Iterable<T> iterable, IFunction1<Boolean, T> match)
            throws Exception
    {
        ArrayList<T> aMachedItems = new ArrayList<T>();
        
        for (Iterator<T> i = iterable.iterator(); i.hasNext();)
        {
            T anItem = i.next();
            
            if (match.invoke(anItem) == true)
            {
                aMachedItems.add(anItem);
            }
        }
        
        return aMachedItems;
    }
    
    public static <T> T firstOrDefault(Iterable<T> iterable, IFunction1<Boolean, T> match)
            throws Exception
    {
        for (Iterator<T> i = iterable.iterator(); i.hasNext();)
        {
            T anItem = i.next();
            
            if (match.invoke(anItem) == true)
            {
                return anItem;
            }
        }
        
        return null;
    }
    
    public static <T> boolean any(Iterable<T> iterable, IFunction1<Boolean, T> match)
            throws Exception
    {
        for (Iterator<T> i = iterable.iterator(); i.hasNext();)
        {
            T anItem = i.next();
            
            if (match.invoke(anItem) == true)
            {
                return true;
            }
        }
        
        return false;
    }

    public static <T> ArrayList<T> toList(Iterable<T> iterable)
    {
        ArrayList<T> aResult = new ArrayList<T>();
        
        for (T anItem : iterable)
        {
            aResult.add(anItem);
        }
        
        return aResult;
    }
}

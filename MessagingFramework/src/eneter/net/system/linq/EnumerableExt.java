package eneter.net.system.linq;

import java.util.ArrayList;
import java.util.Iterator;

import eneter.net.system.IFunction1;

public class EnumerableExt
{
    public static <T> Iterable<T> where(Iterable<T> iterable, IFunction1<Boolean, T> match)
            throws Exception
    {
        ArrayList<T> aMachedItems = new ArrayList<T>();
        
        for (Iterator<T> i = iterable.iterator(); i.hasNext();)
        {
            T anItem = i.next();
            
            if (match.invoke(anItem).booleanValue())
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
            
            if (match.invoke(anItem).booleanValue())
            {
                return anItem;
            }
        }
        
        return null;
    }
}

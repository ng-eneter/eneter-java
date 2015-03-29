package eneter.net.system.collections.generic.internal;

import java.util.AbstractCollection;
import java.util.Iterator;

import eneter.net.system.IFunction1;

class AbstractCollectionExt
{
    public static <T> int remove(AbstractCollection<T> collection, IFunction1<Boolean, T> match)
            throws Exception
        {
            int aNumberOfRemoved = 0;
            
            for (Iterator<T> i = collection.iterator(); i.hasNext();)
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

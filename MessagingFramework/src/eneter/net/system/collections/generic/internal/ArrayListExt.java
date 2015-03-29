package eneter.net.system.collections.generic.internal;

import java.util.ArrayList;

import eneter.net.system.IFunction1;

public class ArrayListExt
{
    public static <T> int removeAll(ArrayList<T> list, IFunction1<Boolean, T> match)
            throws Exception
        {
            return AbstractCollectionExt.remove(list, match);
        }
}

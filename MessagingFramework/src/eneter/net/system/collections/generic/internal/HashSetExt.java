/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
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
        return AbstractCollectionExt.remove(hashSet, match);
    }
}

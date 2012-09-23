/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.internal;

public interface IFunction1<R, T>
{
    R invoke(T t) throws Exception;
}

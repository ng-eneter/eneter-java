/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

public interface IMethod2<T1, T2>
{
    void invoke(T1 t1, T2 t2) throws Exception;
}

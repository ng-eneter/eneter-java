/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Represents the callback method taking two input parameters and returning void.
 *
 * @param <T1> type of the first input parameter.
 * @param <T2> type of the second input parameter.
 */
public interface IMethod2<T1, T2>
{
    /**
     * Callback method.
     * @param t1 first input parameter.
     * @param t2 second input parameter.
     * @throws Exception Implementation of the method is allowed to throw an exception.
     */
    void invoke(T1 t1, T2 t2) throws Exception;
}

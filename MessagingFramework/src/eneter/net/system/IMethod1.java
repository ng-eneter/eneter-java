/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Callback method taking one input parameter of desired type and returning void.
 * It is the equivalent of .NET Action<T>. 
 *
 * @param <T> type of the input parameter.
 */
public interface IMethod1<T>
{
    /**
     * Callback method.
     * @param t input parameter passing to the callback method.
     * @throws Exception Implementation of the method is allowed to throw an exception.
     */
    void invoke(T t) throws Exception;
}

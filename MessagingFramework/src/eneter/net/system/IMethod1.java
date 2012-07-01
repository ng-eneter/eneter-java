/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Represents the callback method taking one input parameter of desired type and returning void.
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

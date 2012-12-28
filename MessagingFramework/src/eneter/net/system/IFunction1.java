/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Callback function taking one input parameter of type T and returning the type R.
 * It is the equivalent of .NET Func<R, T>. 
 * 
 * @param <R> type of the return.
 * @param <T> type of the input parameter.
 */
public interface IFunction1<R, T>
{
    /**
     * Callback function.
     * @param t input paramter of type T.
     * @return returns result of type R.
     * @throws Exception
     */
    R invoke(T t) throws Exception;
}

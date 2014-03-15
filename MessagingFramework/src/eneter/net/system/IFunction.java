/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system;

/**
 * Callback function returning the type R.
 * It is the equivalent of .NET Func<R>.
 *
 * @param <R> type of the return.
 */
public interface IFunction<R>
{
    R invoke() throws Exception;
}

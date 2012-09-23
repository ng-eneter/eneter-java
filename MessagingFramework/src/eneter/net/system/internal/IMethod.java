/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.internal;

/**
 * Represents the callback method that does not take input parameters and returns void.
 *
 */
public interface IMethod
{
    /**
     * Callback method.
     * @throws Exception Implementation of the method is allowed to throw an exception.
     */
    void invoke() throws Exception;
}

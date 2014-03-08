/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.threading.dispatching;

/**
 * Invokes a method according to specified thread mode.
 *
 */
public interface IThreadDispatcher
{
    /**
     * Invokes method in desired thread.
     * @param workItem method to be invoked
     */
    void invoke(Runnable workItem);
}

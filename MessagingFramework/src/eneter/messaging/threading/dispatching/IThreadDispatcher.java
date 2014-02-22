/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.threading.dispatching;

/**
 * Threading mode used by messaging systems to raise events and deliver messages.
 *
 */
public interface IThreadDispatcher
{
    /**
     * Invokes the callback method in desired thread.
     * @param workItem callback method that shall be invoked
     */
    void invoke(Runnable workItem);
}

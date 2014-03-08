/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.threading.dispatching;

/**
 * Provides dispatcher that shall be used for raising events and delivering messages in a correct thread.
 * 
 *
 */
public interface IThreadDispatcherProvider
{
    /**
     * Returns dispatcher that will invoke methods according to its threading model.
     */
    IThreadDispatcher getDispatcher();
}

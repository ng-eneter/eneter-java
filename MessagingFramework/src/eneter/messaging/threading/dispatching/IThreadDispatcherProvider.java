/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.threading.dispatching;

/**
 * Provides dispatcher that shall be used for invoking events and delivering messages in a correct thread.
 * 
 * Having this provider allows to provide the same dispatcher across multiple messaging systems.
 * E.g. if you receive messages from multiple messaging and you want that all of them are delivered in particular thread.
 *
 */
public interface IThreadDispatcherProvider
{
    /**
     * Returns dispatcher that will invoke methods according to its threading model.
     */
    IThreadDispatcher getDispatcher();
}

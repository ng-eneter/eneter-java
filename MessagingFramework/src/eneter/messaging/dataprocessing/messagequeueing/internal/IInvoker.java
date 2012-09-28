/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.messagequeueing.internal;

import eneter.net.system.internal.IMethod;

/**
 * 
 *
 */
public interface IInvoker
{
    void start();

    void stop();

    void invoke(IMethod workItem) throws Exception;
}

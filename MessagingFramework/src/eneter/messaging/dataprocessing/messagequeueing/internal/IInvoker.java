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

package eneter.messaging.threading.dispatching;

public interface IThreadDispatcherProvider
{
    /**
     * Returns dispatcher that can invoke methods according to its threading model.
     */
    IThreadDispatcher getDispatcher();
}

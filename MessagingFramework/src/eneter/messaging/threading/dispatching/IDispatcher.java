package eneter.messaging.threading.dispatching;

public interface IDispatcher
{
    void invoke(Runnable workItem);
}

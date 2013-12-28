package eneter.messaging.threading.dispatching;

public interface IThreadDispatcher
{
    void invoke(Runnable workItem);
}

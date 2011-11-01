package eneter.net.system;

public interface IFunction1<R, T>
{
    R invoke(T t) throws Exception;
}

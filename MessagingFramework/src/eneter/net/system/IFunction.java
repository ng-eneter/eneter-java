package eneter.net.system;

public interface IFunction<R>
{
    R invoke() throws Exception;
}

package eneter.net.system;

public interface IFunction2<R, T1, T2>
{
    R invoke(T1 t1, T2 t2);
}

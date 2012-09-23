package eneter.net.system.internal;

public class Cast
{
    public static <T> T as(Object src, Class<T> dst)
    {
        if (dst.isInstance(src))
        {
            return dst.cast(src);
        }
        
        return null;
    }
}

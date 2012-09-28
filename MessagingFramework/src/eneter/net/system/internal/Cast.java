/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.internal;

/**
 * Internal helper class simulating 'as' cast from .NET. 
 *
 */
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

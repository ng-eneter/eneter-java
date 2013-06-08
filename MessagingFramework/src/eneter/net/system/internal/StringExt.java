/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.net.system.internal;

public final class StringExt
{
	public static boolean isNullOrEmpty(String s)
	{
		return s == null || s.length() == 0;	
	}
}

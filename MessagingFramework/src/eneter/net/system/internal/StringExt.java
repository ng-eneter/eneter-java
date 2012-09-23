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
	public static final Boolean isNullOrEmpty(final String a)
	{
		return a == null || a.length() == 0;	
	}
}

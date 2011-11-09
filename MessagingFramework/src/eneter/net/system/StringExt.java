/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.net.system;

public final class StringExt
{
	public static final Boolean isNullOrEmpty(final String a)
	{
		return a == null || a.length() == 0;	
	}
}

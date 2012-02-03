/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;

public interface ICompositeDuplexOutputChannel extends IDuplexOutputChannel
{
    IDuplexOutputChannel getUnderlyingDuplexOutputChannel();
}

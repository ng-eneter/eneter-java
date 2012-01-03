package eneter.messaging.messagingsystems.composites;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;

public interface ICompositeDuplexOutputChannel extends IDuplexOutputChannel
{
    IDuplexOutputChannel getUnderlyingDuplexOutputChannel();
}

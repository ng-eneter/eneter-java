package eneter.messaging.messagingsystems.composites;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;

public interface ICompositeDuplexInputChannel extends IDuplexInputChannel
{
    IDuplexInputChannel getUnderlyingDuplexInputChannel();
}

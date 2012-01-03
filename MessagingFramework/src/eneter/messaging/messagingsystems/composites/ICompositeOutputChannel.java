package eneter.messaging.messagingsystems.composites;

import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;

public interface ICompositeOutputChannel extends IOutputChannel
{
    IOutputChannel getUnderlyingOutputChannel();
}

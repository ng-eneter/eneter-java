package eneter.messaging.messagingsystems.composites.messagebus;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;

public interface IMessageBus
{
    void attachDuplexInputChannels(IDuplexInputChannel serviceInputChannel, IDuplexInputChannel clientInputChannel) throws Exception;

    void detachDuplexInputChannels();
}

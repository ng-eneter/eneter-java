package eneter.messaging.messagingsystems.composites.messagebus;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.net.system.Event;

public interface IMessageBus
{
    Event<MessageBusServiceEventArgs> serviceConnected();
    Event<MessageBusServiceEventArgs> serviceDisconnected();
    
    void attachDuplexInputChannels(IDuplexInputChannel serviceInputChannel, IDuplexInputChannel clientInputChannel) throws Exception;

    void detachDuplexInputChannels();
    
    String[] getConnectedServices();

    void disconnectService(String serviceAddress);
}

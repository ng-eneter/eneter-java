package eneter.messaging.nodes.loadbalancer;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

public interface ILoadBalancer extends IAttachableDuplexInputChannel
{
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
    
    void addDuplexOutputChannel(String channelId);
    
    void removeDuplexOutputChannel(String channelId);
    
    void removeAllDuplexOutputChannels();
}

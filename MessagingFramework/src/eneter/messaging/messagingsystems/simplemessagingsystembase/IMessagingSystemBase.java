package eneter.messaging.messagingsystems.simplemessagingsystembase;

import eneter.net.system.IMethod1;

public interface IMessagingSystemBase
{
    void sendMessage(String channelId, Object message);
    
    void registerMessageHandler(String channelId, IMethod1<Object> messageHandler);
    
    void unregisterMessageHandler(String channelId);
}

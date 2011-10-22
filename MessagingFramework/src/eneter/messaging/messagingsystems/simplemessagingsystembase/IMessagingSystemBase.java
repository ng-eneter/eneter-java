package eneter.messaging.messagingsystems.simplemessagingsystembase;

import eneter.net.system.IMethod1;

public interface IMessagingSystemBase
{
    void SendMessage(String channelId, Object message);
    
    void RegisterMessageHandler(String channelId, IMethod1<Object> messageHandler);
    
    void UnregisterMessageHandler(String channelId);
}

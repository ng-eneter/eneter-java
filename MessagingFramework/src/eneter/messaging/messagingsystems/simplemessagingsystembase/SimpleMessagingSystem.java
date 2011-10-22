package eneter.messaging.messagingsystems.simplemessagingsystembase;

import eneter.net.system.IMethod1;

public class SimpleMessagingSystem implements IMessagingSystemBase
{
    public SimpleMessagingSystem(IMessagingProvider inputChannelMessaging)
    {
        myInputChannelMessaging = inputChannelMessaging;
    }

    public void SendMessage(String channelId, Object message)
    {
        myInputChannelMessaging.SendMessage(channelId, message);
    }

    public void RegisterMessageHandler(String channelId, IMethod1<Object> messageHandler)
    {
        myInputChannelMessaging.RegisterMessageHandler(channelId, messageHandler);
    }

    public void UnregisterMessageHandler(String channelId)
    {
        myInputChannelMessaging.UnregisterMessageHandler(channelId);
    }
    

    private IMessagingProvider myInputChannelMessaging;
}

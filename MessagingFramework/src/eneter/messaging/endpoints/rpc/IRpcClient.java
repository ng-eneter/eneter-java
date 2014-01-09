package eneter.messaging.endpoints.rpc;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.*;


public interface IRpcClient<TServiceInterface> extends IAttachableDuplexOutputChannel
{
    Event<DuplexChannelEventArgs> connectionOpened();
    
    Event<DuplexChannelEventArgs> connectionClosed();
    
    TServiceInterface getProxy();
    
    <TEventArgs> void subscribeRemoteEvent(String eventName, EventHandler<TEventArgs> eventHandler);
    
    void unsubscribeRemoteEvent(String eventName, EventHandler<?> eventHandler);
    
    Object callRemoteMethod(String methodName, Object[] args) throws Exception;
}

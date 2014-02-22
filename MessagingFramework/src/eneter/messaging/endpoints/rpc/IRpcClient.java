/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.net.system.*;

/**
 * Client which can use Remote Procedure Calls (note: it also works with .NET).
 * 
 * RpcClient acts as a proxy providing the communication functionality allowing a client to call methods exposed by the service.
 *
 * @param <TServiceInterface> Interface exposed by the service.
 */
public interface IRpcClient<TServiceInterface> extends IAttachableDuplexOutputChannel
{
    /**
     * Event raised when the connection with the service is open.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionOpened();
    
    /**
     * Event raised when the connection with the service is closed.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    /**
     * Returns service proxy instance.
     * 
     * @return service proxy implementing the service interface.
     */
    TServiceInterface getProxy();
    
    /**
     * Subscribes to an event from the service.
     * 
     * You can use this method for subscribing if you do not want to use the service proxy.
     * 
     * @param eventName event name.
     * @param eventHandler event handler that will handle events.
     */
    void subscribeRemoteEvent(String eventName, EventHandler<?> eventHandler) throws Exception;
    
    /**
     * Unsubscribes from the event in the service.
     * 
     * You can use this method for unsubscribing if you do not want to use the service proxy.
     * 
     * @param eventName event name.
     * @param eventHandler event handler that shall be unsubscribed.
     */
    void unsubscribeRemoteEvent(String eventName, EventHandler<?> eventHandler) throws Exception;
    
    /**
     * Calls a method in the service.
     * 
     * You can use this method if you do not want to use the service proxy.
     * 
     * @param methodName name of the method that shall be called.
     * @param args method arguments.
     * @return return value. It returns null if the method returns void.
     * @throws Exception
     */
    Object callRemoteMethod(String methodName, Object[] args) throws Exception;
}

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
 * Declares client using remote procedure calls.
 *
 * @param <TServiceInterface> Service interface.
 */
public interface IRpcClient<TServiceInterface> extends IAttachableDuplexOutputChannel
{
    /**
     * Event raised when the connection with the service was open.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionOpened();
    
    /**
     * Event raised when the connection with the service was closed.
     * @return
     */
    Event<DuplexChannelEventArgs> connectionClosed();
    
    /**
     * Returns the proxy for the service.
     * 
     * The returned instance provides the proxy for the service interface.
     * Calling of a method from the proxy will result to the communication with the service.
     * @return
     */
    TServiceInterface getProxy();
    
    /**
     * Subscribes to an event from the service.
     * 
     * Use this method if subscribing via the proxy is not suitable.
     * If the method does not exist in the service interface the exception is thrown.
     * 
     * @param eventName event name.
     * @param eventHandler event handler.
     */
    void subscribeRemoteEvent(String eventName, EventHandler<?> eventHandler) throws Exception;
    
    /**
     * Unsubscribes from the event in the service.
     * 
     * Use this method if unsubscribing via the proxy is not suitable.
     * If the method does not exist in the service interface the exception is thrown.
     * 
     * @param eventName event name.
     * @param eventHandler event handler tha shall be unsubscribed.
     */
    void unsubscribeRemoteEvent(String eventName, EventHandler<?> eventHandler) throws Exception;
    
    /**
     * Calls the method in the service.
     * 
     * Use this method if unsubscribing via the proxy is not suitable.
     * If the method does not exist in the service interface the exception is thrown.
     * 
     * @param methodName name of the method that shall be called.
     * @param args method arguments.
     * @return return value. It returns null if the method returns void.
     * @throws Exception
     */
    Object callRemoteMethod(String methodName, Object[] args) throws Exception;
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.loadbalancer;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.Event;

/**
 * Load balancer.
 *
 *
 * The load balancer maintains a list of receivers processing a certain request.
 * When the balancer receives the request, it chooses which receiver shall process it,
 * so that all receivers are loaded optimally.
 *
 */
public interface ILoadBalancer extends IAttachableDuplexInputChannel
{
    /**
     * The event is invoked when the client sending requests was connected to the load balancer.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    /**
     * The event is invoked when the client sending requests was disconnected from the load balanacer.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
    
    /**
     * Adds the request receiver to the load balancer.
     * @param channelId channel id (address) of the receiver processing requests.
     * @throws Exception
     */
    void addDuplexOutputChannel(String channelId) throws Exception;
    
    /**
     * Removes the request receiver from the load balancer.
     * @param channelId channel id (address) of the receiver processing requests.
     * @throws Exception
     */
    void removeDuplexOutputChannel(String channelId) throws Exception;
    
    /**
     * Removes all request receiers from the load balanacer.
     */
    void removeAllDuplexOutputChannels();
}

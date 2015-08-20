/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

/**
 * Callback which is called by broker to authorize publish, subscribe or unsubscribe operation.
 *
 */
public interface AuthorizeBrokerRequestCallback
{
    /**
     * Performs authorizing of Publish, Subscribe or Unsubscribe  
     * @param responseReceiverId id of client which sent the request.
     * @param request request sent by the client.
     * @return true if the request is valid, false if the request is invalid. If it returns false the request
     *  will not be invoked and the client will be disconnected.
     */
    boolean invoke(String responseReceiverId, BrokerMessage request);
}

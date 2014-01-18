/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

import eneter.net.system.Event;
import eneter.net.system.EventArgs;

/**
 * Declares factory which can create RPC clients and services. 
 * 
 *
 */
public interface IRpcFactory
{
    /**
     * Creates RPC client for the given interface.
     * 
     * 
     * 
     * 
     * @param clazz Type of the service interface.
     * @return
     */
    <TServiceInterface> IRpcClient<TServiceInterface> createClient(Class<TServiceInterface> clazz);
    
    /**
     * Creates RPC service for the given interface.
     * @param service
     * @param clazz
     * @return
     */
    <TServiceInterface> IRpcService<TServiceInterface> createService(TServiceInterface service, Class<TServiceInterface> clazz);
 
}

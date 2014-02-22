/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;


/**
 * Creates services and clients that can communicate using Remote Procedure Calls.
 *
 */
public interface IRpcFactory
{
    /**
     * Creates RPC client for the given interface.
     * 
     * @param clazz service interface type.
     * @return RpcClient instance
     */
    <TServiceInterface> IRpcClient<TServiceInterface> createClient(Class<TServiceInterface> clazz);
    
    /**
     * Creates RPC service for the given interface.
     * 
     * @param service instance implementing the given service interface.
     * @param clazz service interface type.
     * @return RpcService instance.
     */
    <TServiceInterface> IRpcService<TServiceInterface> createService(TServiceInterface service, Class<TServiceInterface> clazz);
 }

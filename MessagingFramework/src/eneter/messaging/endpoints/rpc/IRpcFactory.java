/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

import eneter.net.system.IFunction;


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
     * Creates single-instance RPC service for the given interface.
     * 
     * Single-instance means that there is one instance of the service shared by all clients.
     * 
     * @param service instance implementing the given service interface.
     * @param clazz service interface type.
     * @return RpcService instance.
     */
    <TServiceInterface> IRpcService<TServiceInterface> createSingleInstanceService(TServiceInterface service, Class<TServiceInterface> clazz);
    
    /**
     * Creates per-client-instance RPC service for the given interface.
     * 
     * Per-client-instance means that for each connected client is created a separate instace of the service.
     * 
     * @param serviceFactoryMethod factory method used to create the service instance when the client is connected
     * @param clazz service interface type
     * @return RpcService instance
     */
    <TServiceInterface> IRpcService<TServiceInterface> createPerClientInstanceService(IFunction<TServiceInterface> serviceFactoryMethod, Class<TServiceInterface> clazz);
 }

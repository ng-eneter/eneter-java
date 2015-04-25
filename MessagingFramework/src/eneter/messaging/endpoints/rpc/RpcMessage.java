/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

import java.io.Serializable;

/**
 * Internal message used for the communication between RpcClient and RpcService.
 *
 */
public class RpcMessage implements Serializable
{
    /**
     * Identifies the request on the client side.
     */
    public int Id;

    /**
     * Identifies the type of the request/response message.
     * e.g. if it is InvokeMethod, MethodResponse, SubscribeEvent, UnsubscribeEvent, RaiseEvent.
     */
    public int Flag;

    /**
     * The name of the operation that shall be performed.
     * e.g. in case of InvokeMethod it specifies which method shall be invoked.
     */
    public String OperationName;

    /**
     * Message data.
     * e.g. in case of InvokeMethod it contains input parameters data.
     */
    public Object[] SerializedData;

    /**
     * If an error occurred in the service.
     */
    public String ErrorType;
    
    public String ErrorMessage;
    
    public String ErrorDetails;

    private static final long serialVersionUID = 8365985506571587359L;
}

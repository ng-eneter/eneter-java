/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

/**
 * Exception thrown if an RPC call fails on the service side.
 * E.g. in case the service method throws an exception it is transfered to the client.
 * When the client receives the exception from the service it creates RpcException and stores there all details
 * about original service exception. The RpcException is then thrown and can be processed by the client.  
 *
 */
public class RpcException extends RuntimeException
{
    /**
     * Constructs the exception.
     * @param message original message from the service.
     * @param serviceExceptionType name of the exception type thrown in the service.
     * @param serviceExceptionDetails exception details including the callstack.
     */
    public RpcException(String message, String serviceExceptionType, String serviceExceptionDetails)
    {
        super(message);
        myServiceExceptionType = serviceExceptionType;
        myServiceExceptionDetails = serviceExceptionDetails;
    }
    
    /**
     * Gets name of the exception type thrown in the service.
     * @return name of the exception type
     */
    public String getServiceExceptionType()
    {
        return myServiceExceptionType;
    }
    
    /**
     * Gets service exception details including callstack.
     * @return exception details.
     */
    public String getServiceExceptionDetails()
    {
        return myServiceExceptionDetails;
    }
    
    private String myServiceExceptionType;
    private String myServiceExceptionDetails;
    private static final long serialVersionUID = 6217530926330712009L;
}

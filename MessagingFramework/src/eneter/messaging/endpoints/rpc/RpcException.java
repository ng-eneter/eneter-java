/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

public class RpcException extends RuntimeException
{
    public RpcException(String message, String serviceExceptionType, String serviceExceptionDetails)
    {
        super(message);
        myServiceExceptionType = serviceExceptionType;
        myServiceExceptionDetails = serviceExceptionDetails;
    }
    

    public String getServiceExceptionType()
    {
        return myServiceExceptionType;
    }
    
    public String getServiceExceptionDetails()
    {
        return myServiceExceptionDetails;
    }
    
    private String myServiceExceptionType;
    private String myServiceExceptionDetails;
    private static final long serialVersionUID = 6217530926330712009L;
}

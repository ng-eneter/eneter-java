/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

/**
 * Internal commands for interaction via RPC.
 *
 */
public enum ERpcRequest
{
    /**
     * Client invokes a method.
     */
    InvokeMethod(10),
    
    /**
     * Client subscribes an event.
     */
    SubscribeEvent(20),
    
    /**
     * Client unsubscribes an event.
     */
    UnsubscribeEvent(30),
    
    /**
     *  Service raises an event.
     */
    RaiseEvent(40),
    
    /**
     * RPC service sends back a response for 'InvokeMethod', 'SubscribeEvent' or 'UnsubscribeEvent'.
     */
    Response(50);
    
    
    /**
     * Converts enum to the integer value.
     * @return
     */
    public int geValue()
    {
        return myValue;
    }
    
    /**
     * Converts integer value to the enum.
     * @param i value
     * @return enum
     */
    public static ERpcRequest fromInt(int i)
    {
        switch (i)
        {
            case 10: return InvokeMethod;
            case 20: return SubscribeEvent;
            case 30: return UnsubscribeEvent;
            case 40: return RaiseEvent;
            case 50: return Response;
        }
        return null;
    }
    
    private ERpcRequest(int value)
    {
        myValue = value;
    }

    private final int myValue;
}

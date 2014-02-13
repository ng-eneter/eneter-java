/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

public class MessageBusServiceEventArgs
{
    public MessageBusServiceEventArgs(String serviceAdddress)
    {
        myServiceAddress = serviceAdddress;
    }

    public String getServiceAddress()
    {
        return myServiceAddress;
    }
    
    private String myServiceAddress;
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.net.InetAddress;

class IPAddressExt
{
    public static InetAddress parseMulticastGroup(String multicastGroup) throws Exception
    {
        InetAddress aMulticastIpAddress = InetAddress.getByName(multicastGroup);
        if (!aMulticastIpAddress.isMulticastAddress())
        {
            throw new IllegalStateException("'" + multicastGroup.toString() + "' is not a multicast IP address."); 
        }
        
        return aMulticastIpAddress;
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem.internal;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.internal.Cast;

public class IpAddressUtil
{
    public static String getLocalIpAddress(Socket socket)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Get IP address of this end-point.
            SocketAddress anEndPoint = socket.getLocalSocketAddress();
            return getIpAddress(anEndPoint);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static String getRemoteIpAddress(Socket socket)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Get IP address of connected end-point.
            SocketAddress anEndPoint = socket.getRemoteSocketAddress();
            return getIpAddress(anEndPoint);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    //public static String getIpAddress(InetSocketAddress address)
    //{
    //    return (address != null) ? getIpAddress(address.getAddress()) : "";
    //}
    
    //public static String getIpAddress(InetAddress address)
    //{
    //    String anIpAddress = (address != null) ? address.toString() : "";
    //    return anIpAddress;
    //}
    
    private static String getIpAddress(SocketAddress socketAddress)
    {
        InetSocketAddress aInetSocketAddress = Cast.as(socketAddress, InetSocketAddress.class);
        return aInetSocketAddress.toString();
    }
}

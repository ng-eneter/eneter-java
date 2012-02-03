/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import eneter.messaging.diagnostic.EneterTrace;

public class NoneSecurityServerFactory implements IServerSecurityFactory
{
    @Override
    public ServerSocket createServerSocket(InetSocketAddress socketAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ServerSocket aServerSocket = new ServerSocket();
            aServerSocket.bind(socketAddress, 1000);
            return aServerSocket;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
}

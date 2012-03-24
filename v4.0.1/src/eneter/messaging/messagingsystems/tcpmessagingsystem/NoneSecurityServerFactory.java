/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements factory for the server socket that does not use any security.
 *
 */
public class NoneSecurityServerFactory implements IServerSecurityFactory
{
    /**
     * Creates non-secured server socket.
     */
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

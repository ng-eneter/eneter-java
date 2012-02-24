/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetSocketAddress;
import java.net.Socket;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements factory for the client socket that does not use any security.
 * 
 *
 */
public class NoneSecurityClientFactory implements IClientSecurityFactory
{
    /**
     * Constructs the factory that creates a normal socket on the client side.
     * The connection timeout is set to 3000 milliseconds.
     */
    public NoneSecurityClientFactory()
    {
        this(3000);
    }
    
    /**
     * Constructs the factory that creates a normal socket on the client side.
     * 
     * @param connectionTimeout timeout in milliseconds for opening connection 
     */
    public NoneSecurityClientFactory(int connectionTimeout)
    {
        myConnectionTimeout = connectionTimeout;
    }
    
    /**
     * Creates non-secured client socket.
     */
    @Override
    public Socket createClientSocket(InetSocketAddress socketAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Socket aClientSocket = new Socket();
            
            // Connect with the timeout 3 seconds.
            aClientSocket.connect(socketAddress, myConnectionTimeout);
            aClientSocket.setTcpNoDelay(true);
            return aClientSocket;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private int myConnectionTimeout;
}

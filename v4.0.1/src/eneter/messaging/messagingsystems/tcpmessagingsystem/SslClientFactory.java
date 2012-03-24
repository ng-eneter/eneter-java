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

import javax.net.ssl.*;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements factory that creates SSL client socket.
 *
 */
public class SslClientFactory implements IClientSecurityFactory
{
    /**
     * Constructs the factory.
     * 
     * The factory will use the default socket factory returned from SSLSocketFactory.getDefault()
     */
    public SslClientFactory()
    {
        this((SSLSocketFactory)SSLSocketFactory.getDefault());
    }
    
    /**
     * Constructs the factory.
     * 
     * The factory will internally use given SSLSocketFactory.
     * 
     * @param socketFactory
     */
    public SslClientFactory(SSLSocketFactory socketFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySocketFactory = socketFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates SSLClientSocket
     */
    @Override
    public Socket createClientSocket(InetSocketAddress socketAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SSLSocket aClientSocket = (SSLSocket)mySocketFactory.createSocket(socketAddress.getAddress(), socketAddress.getPort());
            aClientSocket.startHandshake();
            
            return aClientSocket;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private SSLSocketFactory mySocketFactory;
}

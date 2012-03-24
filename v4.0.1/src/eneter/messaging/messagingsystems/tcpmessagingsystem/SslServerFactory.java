/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.*;

import javax.net.ssl.*;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements factory that creates SSL Server Sockets.
 * 
 *
 */
public class SslServerFactory implements IServerSecurityFactory
{
    /**
     * Constructs the factory.
     * 
     * The factory will use SSLServerSocketFactory.getDefault().
     */
    public SslServerFactory()
    {
        this((SSLServerSocketFactory)SSLServerSocketFactory.getDefault(), false);
    }
    
    /**
     * Constructs the factory.
     * 
     * @param sslServerSocketFactory given SSL server socket factory
     * @param isClientCertificateRequired true if also the client certificate shall be required during the communication
     */
    public SslServerFactory(SSLServerSocketFactory sslServerSocketFactory, boolean isClientCertificateRequired)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySslServerSocketFactory = sslServerSocketFactory;
            myIsClientCertificateRequired = isClientCertificateRequired;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the SSLServerSocket.
     */
    @Override
    public ServerSocket createServerSocket(InetSocketAddress socketAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SSLServerSocket aServerSocket = (SSLServerSocket)mySslServerSocketFactory.createServerSocket(socketAddress.getPort(), 1000, socketAddress.getAddress());
            aServerSocket.setNeedClientAuth(myIsClientCertificateRequired);
            
            return aServerSocket;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private boolean myIsClientCertificateRequired;
    private SSLServerSocketFactory mySslServerSocketFactory;
}

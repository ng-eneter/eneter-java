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

public class SslServerFactory implements IServerSecurityFactory
{
    public SslServerFactory()
    {
        this((SSLServerSocketFactory)SSLServerSocketFactory.getDefault(), false);
    }
    
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

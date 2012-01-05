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
    public ServerSocket createServerSocket(InetAddress address, int port) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SSLServerSocket aServerSocket = (SSLServerSocket)mySslServerSocketFactory.createServerSocket(port, 1000, address);
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

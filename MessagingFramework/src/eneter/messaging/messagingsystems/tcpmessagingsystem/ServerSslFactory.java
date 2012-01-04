package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.*;

import javax.net.ssl.*;

public class ServerSslFactory implements IServerSecurityFactory
{
    public ServerSslFactory()
    {
        this((SSLServerSocketFactory)SSLServerSocketFactory.getDefault(), false);
    }
    
    public ServerSslFactory(SSLServerSocketFactory sslServerSocketFactory, boolean isClientCertificateRequired)
    {
        mySslServerSocketFactory = sslServerSocketFactory;
        myIsClientCertificateRequired = isClientCertificateRequired;
    }
    
    
    @Override
    public ServerSocket createServerSocket(InetAddress address, int port) throws Exception
    {
        SSLServerSocket aServerSocket = (SSLServerSocket)mySslServerSocketFactory.createServerSocket(port, 1000, address);
        aServerSocket.setNeedClientAuth(myIsClientCertificateRequired);
        
        return aServerSocket;
    }

    private boolean myIsClientCertificateRequired;
    private SSLServerSocketFactory mySslServerSocketFactory;
}

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.*;

public class ClientSslFactory implements IClientSecurityFactory
{
    public ClientSslFactory()
    {
        this((SSLSocketFactory)SSLSocketFactory.getDefault());
    }
    
    public ClientSslFactory(SSLSocketFactory socketFactory)
    {
        mySocketFactory = socketFactory;
    }
    
    @Override
    public Socket createClientSocket(InetAddress address, int port) throws Exception
    {
        SSLSocket aClientSocket = (SSLSocket)mySocketFactory.createSocket(address, port);
        aClientSocket.startHandshake();
        
        return null;
    }

    private SSLSocketFactory mySocketFactory;
}

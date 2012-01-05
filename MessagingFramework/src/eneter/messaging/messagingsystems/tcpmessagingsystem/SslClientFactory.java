package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.*;

import eneter.messaging.diagnostic.EneterTrace;

public class SslClientFactory implements IClientSecurityFactory
{
    public SslClientFactory()
    {
        this((SSLSocketFactory)SSLSocketFactory.getDefault());
    }
    
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
    
    @Override
    public Socket createClientSocket(InetAddress address, int port) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SSLSocket aClientSocket = (SSLSocket)mySocketFactory.createSocket(address, port);
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

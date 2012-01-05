package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.Socket;

import eneter.messaging.diagnostic.EneterTrace;

public class NoneSecurityClientFactory implements IClientSecurityFactory
{
    @Override
    public Socket createClientSocket(InetAddress address, int port) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Socket aClientSocket = new Socket(address, port);
            aClientSocket.setTcpNoDelay(true);
            return aClientSocket;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
}

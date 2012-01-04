package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.Socket;

class ClientNonSecurityFactory implements IClientSecurityFactory
{
    @Override
    public Socket createClientSocket(InetAddress address, int port) throws Exception
    {
        Socket aClientSocket = new Socket(address, port);
        aClientSocket.setTcpNoDelay(true);
        return aClientSocket;
    }
}

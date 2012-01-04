package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.ServerSocket;

class ServerNoneSecurityFactory implements IServerSecurityFactory
{
    @Override
    public ServerSocket createServerSocket(InetAddress address, int port) throws Exception
    {
        ServerSocket aServerSocket = new ServerSocket(port, 1000, address);
        return aServerSocket;
    }
}

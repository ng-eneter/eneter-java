package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.ServerSocket;

public interface IServerSecurityFactory
{
    ServerSocket createServerSocket(InetAddress address, int port) throws Exception;
}

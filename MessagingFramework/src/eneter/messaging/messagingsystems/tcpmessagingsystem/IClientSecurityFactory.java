package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.Socket;

public interface IClientSecurityFactory
{
    Socket createClientSocket(InetAddress address, int port) throws Exception;
}

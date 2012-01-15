package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.*;

public interface IClientSecurityFactory
{
    Socket createClientSocket(InetSocketAddress socketAddress) throws Exception;
}

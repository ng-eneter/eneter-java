package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.*;

public interface IServerSecurityFactory
{
    ServerSocket createServerSocket(InetSocketAddress socketAddress) throws Exception;
}

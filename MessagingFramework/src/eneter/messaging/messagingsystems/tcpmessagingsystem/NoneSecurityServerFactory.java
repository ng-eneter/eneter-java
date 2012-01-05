package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.ServerSocket;

import eneter.messaging.diagnostic.EneterTrace;

public class NoneSecurityServerFactory implements IServerSecurityFactory
{
    @Override
    public ServerSocket createServerSocket(InetAddress address, int port) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ServerSocket aServerSocket = new ServerSocket(port, 1000, address);
            return aServerSocket;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
}

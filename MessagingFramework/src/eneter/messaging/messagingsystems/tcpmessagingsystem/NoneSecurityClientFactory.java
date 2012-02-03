/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import eneter.messaging.diagnostic.EneterTrace;

public class NoneSecurityClientFactory implements IClientSecurityFactory
{
    @Override
    public Socket createClientSocket(InetSocketAddress socketAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Socket aClientSocket = new Socket();
            aClientSocket.connect(socketAddress);
            aClientSocket.setTcpNoDelay(true);
            return aClientSocket;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.*;

/**
 * Declares the factory responsible for creating the security client socket.
 * 
 */
public interface IClientSecurityFactory
{
    /**
     * Creates the client socket.
     * 
     * @param socketAddress address
     * @return client socket
     * @throws Exception
     */
    Socket createClientSocket(InetSocketAddress socketAddress) throws Exception;
}

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
 * Declares the factory responsible for creating the security server socket.
 * 
 *
 */
public interface IServerSecurityFactory
{
    /**
     * Creates the server socket.
     * 
     * @param socketAddress address
     * @return server socket
     * @throws Exception
     */
    ServerSocket createServerSocket(InetSocketAddress socketAddress) throws Exception;
}

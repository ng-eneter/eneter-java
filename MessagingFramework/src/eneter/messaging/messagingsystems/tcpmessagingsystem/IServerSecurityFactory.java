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
 * Creates server socket.
 * 
 * The factory is used by TcpMessagingSystem to create the server socket.
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
    
    /**
     * Sets timeout for sending a response message. 0 means infinite time (use 30000 by default)
     * @param sendTimeout
     */
    void setSendTimeout(int sendTimeout);
    
    /**
     * Returns timeout setup for sending a response message.
     * @return sending timeout in milliseconds
     */
    int getSendTimeout();
    
    /**
     * Sets timeout for receiving a message. 0 means infinite time (use 30000 by default)
     * @param receiveTimeout
     */
    void setReceiveTimeout(int receiveTimeout);
    
    /**
     * Returns timeout setup for receiving a message.
     * @return receiving timeout in milliseconds
     */
    int getReceiveTimeout();
    
    /**
     * Sets the size of sending buffer in bytes.
     * @param bufferSize size of the buffer in bytes. (use 8192 by default)
     */
    void setSendBufferSize(int bufferSize);
    
    /**
     * Returns the size of the sending buffer in bytes. 
     * @return size of the sending buffer
     */
    int getSendBufferSize();
    
    /**
     * Sets the size of receiving buffer in bytes.
     * @param bufferSize size of the buffer in bytes. (use 8192 by default)
     */
    void setReceiveBufferSize(int bufferSize);
    
    /**
     * Returns the size of the receiving buffer in bytes.
     * @return size of the receiving buffer 
     */
    int getReceiveBufferSize();
    
    /**
     * Sets the flag indicating whether the socket can be bound to the address which is already in use.
     * @param allowReuseAddress true if the socket can be bound to the address which is already in use.
     */
    void setReuseAddress(boolean allowReuseAddress);
    
    /**
     * Gets the flag indicating whether the socket can be bound to the address which is already in use.
     * @return true if the socket can be bound to the address which is already in use.
     */
    boolean getReuseAddress();
    
    /**
     * Gets the maximum amount of connections the TCP listener can handle.
     * The default value is -1 which means the the amount of connections is not restricted.
     * @return amount of connections
     */
    int getMaxAmountOfConnections();
    
    /**
     * Sets the maximum amount of connections the TCP listener can handle.
     * If the value is -1 the amount of connections is not restricted.
     * @param maxAmountOfConnections
     */
    void setMaxAmountOfConnections(int maxAmountOfConnections);
}

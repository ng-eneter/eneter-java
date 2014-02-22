/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.*;

/**
 * Creates client socket.
 * 
 * The factory is used by TcpMessagingSystem to create the client socket.
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
    
    /**
     * Sets open connection timeout in milliseconds.
     * @param connectionTimeout timeout in milliseconds. 0 means infinite time (use 30000 by default)
     */
    void setConnectionTimeout(int connectionTimeout);
    
    /**
     * Returns open connection timeout.
     * @return
     */
    int getConnectionTimeout();
    
    /**
     * Sets timeout for sending a message. 0 means infinite time (use 30000 by default)
     * @param sendTimeout
     */
    void setSendTimeout(int sendTimeout);
    
    /**
     * Returns timeout setup for sending a message.
     * @return
     */
    int getSendTimeout();
    
    /**
     * Sets timeout for receiving a message. 0 means infinite time (use 30000 by default)
     * @param receiveTimeout
     */
    void setReceiveTimeout(int receiveTimeout);
    
    /**
     * Returns timeout setup for receiving a message.
     * @return
     */
    int getReceiveTimeout();
    
    /**
     * Sets the size of sending buffer in bytes.
     * @param bufferSize size of the buffer in bytes. (use 8192 by default)
     */
    void setSendBufferSize(int bufferSize);
    
    /**
     * Returns the size of the sending buffer in bytes. 
     * @return
     */
    int getSendBufferSize();
    
    /**
     * Sets the size of receiving buffer in bytes.
     * @param bufferSize size of the buffer in bytes. (use 8192 by default)
     */
    void setReceiveBufferSize(int bufferSize);
    
    /**
     * Returns the size of the receiving buffer in bytes.
     * @return
     */
    int getReceiveBufferSize();
}

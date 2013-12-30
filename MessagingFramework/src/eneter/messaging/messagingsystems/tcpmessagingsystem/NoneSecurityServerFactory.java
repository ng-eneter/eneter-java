/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements factory for the server socket that does not use any security.
 *
 */
public class NoneSecurityServerFactory implements IServerSecurityFactory
{
    /**
     * Constructs the factory that creates a normal server socket with default values.
     * The sending timeout is set to 30000 milliseconds.
     * The receiving timeout is set to infinite time.
     * The message sending buffer is set to 8192 bytes.
     * The message receiving buffer is set to 8192 bytes.
     */
    public NoneSecurityServerFactory()
    {
        mySendTimeout = 30000;
        myReceiveTimeout = 0; // infinite
        mySendBuffer = 8192;
        myReceiveBuffer = 8192;
    }
    
    /**
     * Creates non-secured server socket.
     */
    @Override
    public ServerSocket createServerSocket(InetSocketAddress socketAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ServerSocket aServerSocket = new ServerSocket();
            aServerSocket.setReceiveBufferSize(myReceiveBuffer);
            aServerSocket.bind(socketAddress, 1000);
            return aServerSocket;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void setSendTimeout(int sendTimeout)
    {
        mySendTimeout = sendTimeout;
    }


    @Override
    public int getSendTimeout()
    {
        return mySendTimeout;
    }


    @Override
    public void setReceiveTimeout(int receiveTimeout)
    {
        myReceiveTimeout = receiveTimeout;
        
    }


    @Override
    public int getReceiveTimeout()
    {
        return myReceiveTimeout;
    }
    
    
    @Override
    public void setSendBufferSize(int size)
    {
        mySendBuffer = size;
    }
    
    
    @Override
    public int getSendBufferSize()
    {
        return mySendBuffer;
    }
    
    
    @Override
    public void setReceiveBufferSize(int size)
    {
        myReceiveBuffer = size;
    }
    
    
    @Override
    public int getReceiveBufferSize()
    {
        return myReceiveBuffer;
    }
    
    private int mySendTimeout;
    private int myReceiveTimeout;
    private int mySendBuffer;
    private int myReceiveBuffer;
}

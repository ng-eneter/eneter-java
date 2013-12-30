/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetSocketAddress;
import java.net.Socket;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Implements factory for the client socket that does not use any security.
 * 
 *
 */
public class NoneSecurityClientFactory implements IClientSecurityFactory
{
    /**
     * Constructs the factory that creates a normal client socket with default values.
     * The connection timeout is set to 30000 milliseconds.
     * The sending timeout is set to infinite time.
     * The receiving timeout is set to infinite time.
     * The message sending buffer is set to 8192 bytes.
     * The message receiving buffer is set to 8192 bytes.
     */
    public NoneSecurityClientFactory()
    {
        myConnectionTimeout = 30000;
        mySendTimeout = 0; // infinite
        myReceiveTimeout = 0; // infinite
        mySendBuffer = 8192;
        myReceiveBuffer = 8192;
    }
    
    
    /**
     * Creates non-secured client socket.
     */
    @Override
    public Socket createClientSocket(InetSocketAddress socketAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Socket aClientSocket = new Socket();
            aClientSocket.setTcpNoDelay(true);
            aClientSocket.setSendBufferSize(mySendBuffer);
            aClientSocket.setReceiveBufferSize(myReceiveBuffer);
            aClientSocket.setSoTimeout(myReceiveTimeout);
            
            // Connect with the timeout.
            aClientSocket.connect(socketAddress, myConnectionTimeout);
            
            return aClientSocket;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    @Override
    public void setConnectionTimeout(int connectionTimeout)
    {
        myConnectionTimeout = connectionTimeout;
    }
    
    
    @Override
    public int getConnectionTimeout()
    {
        return myConnectionTimeout;
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
    
    private int myConnectionTimeout;
    private int mySendTimeout;
    private int myReceiveTimeout;
    private int mySendBuffer;
    private int myReceiveBuffer;
    
}

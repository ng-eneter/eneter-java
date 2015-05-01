/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.*;

import javax.net.ssl.*;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Creates SSL Server Sockets.
 * 
 *
 */
public class SslServerFactory implements IServerSecurityFactory
{
    /**
     * Constructs the factory.
     * The sending timeout is set to infinite time.
     * The receiving timeout is set to infinite time.
     * The message sending buffer is set to 8192 bytes.
     * The message receiving buffer is set to 8192 bytes.
     * 
     * The factory will use SSLServerSocketFactory.getDefault().
     */
    public SslServerFactory()
    {
        this((SSLServerSocketFactory)SSLServerSocketFactory.getDefault(), false);
    }
    
    /**
     * Constructs the factory.
     * The sending timeout is set to infinite time.
     * The receiving timeout is set to infinite time.
     * The message sending buffer is set to 8192 bytes.
     * The message receiving buffer is set to 8192 bytes.
     * 
     * @param sslServerSocketFactory given SSL server socket factory
     * @param isClientCertificateRequired true if also the client certificate shall be required during the communication
     */
    public SslServerFactory(SSLServerSocketFactory sslServerSocketFactory, boolean isClientCertificateRequired)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySslServerSocketFactory = sslServerSocketFactory;
            myIsClientCertificateRequired = isClientCertificateRequired;
            
            mySendTimeout = 0; // infinite
            myReceiveTimeout = 0; // infinite
            mySendBuffer = 8192;
            myReceiveBuffer = 8192;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the SSLServerSocket.
     */
    @Override
    public ServerSocket createServerSocket(InetSocketAddress socketAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SSLServerSocket aServerSocket = (SSLServerSocket)mySslServerSocketFactory.createServerSocket();
            aServerSocket.setReceiveBufferSize(myReceiveBuffer);
            aServerSocket.bind(socketAddress, 1000);
            
            aServerSocket.setNeedClientAuth(myIsClientCertificateRequired);
            
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
    
    private boolean myIsClientCertificateRequired;
    private SSLServerSocketFactory mySslServerSocketFactory;
    
    private int mySendTimeout;
    private int myReceiveTimeout;
    private int mySendBuffer;
    private int myReceiveBuffer;
}

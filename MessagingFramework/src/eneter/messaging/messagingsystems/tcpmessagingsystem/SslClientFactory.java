/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.*;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Creates SSL client socket.
 *
 */
public class SslClientFactory implements IClientSecurityFactory
{
    /**
     * Constructs the factory.
     * 
     * The factory will use the default socket factory returned from SSLSocketFactory.getDefault()
     * The connection timeout is set to 30000 milliseconds.
     * The sending timeout is set to infinite time.
     * The receiving timeout is set to infinite time.
     * The message sending buffer is set to 8192 bytes.
     * The message receiving buffer is set to 8192 bytes.
     */
    public SslClientFactory()
    {
        this((SSLSocketFactory)SSLSocketFactory.getDefault());
    }
    
    /**
     * Constructs the factory.
     * 
     * The factory will internally use given SSLSocketFactory.
     * The connection timeout is set to 30000 milliseconds.
     * The sending timeout is set to infinite time.
     * The receiving timeout is set to infinite time.
     * The message sending buffer is set to 8192 bytes.
     * The message receiving buffer is set to 8192 bytes.
     * 
     * @param socketFactory
     */
    public SslClientFactory(SSLSocketFactory socketFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySocketFactory = socketFactory;
            myConnectionTimeout = 30000;
            mySendTimeout = 0; // infinite
            myReceiveTimeout = 0; // infinite
            mySendBuffer = 8192;
            myReceiveBuffer = 8192;
            myReuseAddressFlag = false;
            myResponseReceivingPort = -1;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates SSLClientSocket
     */
    @Override
    public Socket createClientSocket(InetSocketAddress socketAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            SSLSocket aClientSocket = (SSLSocket)mySocketFactory.createSocket();
            aClientSocket.setTcpNoDelay(true);
            aClientSocket.setSendBufferSize(mySendBuffer);
            aClientSocket.setReceiveBufferSize(myReceiveBuffer);
            aClientSocket.setSoTimeout(myReceiveTimeout);
            aClientSocket.setReuseAddress(myReuseAddressFlag);
            
            if (myResponseReceivingPort > 0)
            {
                InetSocketAddress aDummyIpAddress = new InetSocketAddress("0.0.0.0", myResponseReceivingPort);
                aClientSocket.bind(aDummyIpAddress);
            }
            
            // Connect with the timeout.
            aClientSocket.connect(socketAddress, myConnectionTimeout);
            
            // SSL handshaking.
            aClientSocket.startHandshake();
            
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
    
    @Override
    public void setReuseAddress(boolean allowReuseAddress)
    {
        myReuseAddressFlag = allowReuseAddress;
    }

    @Override
    public boolean getReuseAddress()
    {
        return myReuseAddressFlag;
    }

    @Override
    public void setResponseReceiverPort(int port)
    {
        myResponseReceivingPort = port;
    }

    @Override
    public int getResponseReceiverPort()
    {
        return myResponseReceivingPort;
    }
    
    
    private SSLSocketFactory mySocketFactory;
    
    private int myConnectionTimeout;
    private int mySendTimeout;
    private int myReceiveTimeout;
    private int mySendBuffer;
    private int myReceiveBuffer;
    private boolean myReuseAddressFlag;
    private int myResponseReceivingPort;
}

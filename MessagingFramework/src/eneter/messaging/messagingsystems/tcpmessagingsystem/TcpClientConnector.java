/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.IpAddressUtil;
import eneter.net.system.*;
import eneter.net.system.threading.internal.*;


class TcpClientConnector implements IOutputConnector
{
    public TcpClientConnector(String ipAddressAndPort, IClientSecurityFactory clientSecurityFactory) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            URI aUri;
            try
            {
                aUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(ipAddressAndPort + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            mySocketAddress = new InetSocketAddress(aUri.getHost(), aUri.getPort());
            myClientSecurityFactory = clientSecurityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void openConnection(
            IFunction1<Boolean, MessageContext> responseMessageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOpenConnectionLock)
            {
                if (isConnected())
                {
                    throw new IllegalStateException(TracedObject() + ErrorHandler.IsAlreadyConnected);
                }

                try
                {
                    myTcpClient = myClientSecurityFactory.createClientSocket(mySocketAddress);
                    
                    myIpAddress = IpAddressUtil.getLocalIpAddress(myTcpClient);

                    // If it shall listen to response messages.
                    if (responseMessageHandler != null)
                    {
                        myStopReceivingRequestedFlag = false;

                        myResponseMessageHandler = responseMessageHandler;

                        myResponseReceiverThread = new Thread(myDoResponseListening);
                        myResponseReceiverThread.start();

                        // Wait until thread listening to response messages is running.
                        myListeningToResponsesStartedEvent.waitOne(1000);
                    }
                }
                catch (Exception err)
                {
                    closeConnection();
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOpenConnectionLock)
            {
                myStopReceivingRequestedFlag = true;

                if (myTcpClient != null)
                {
                    try
                    {
                        // This will close the connection with the server and it should
                        // also release the thread waiting for a response message.
                        myTcpClient.close();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to stop Tcp connection.", err);
                    }
                    myTcpClient = null;
                }
                
                if (myResponseReceiverThread != null && myResponseReceiverThread.getState() != Thread.State.NEW)
                {
                    try
                    {
                        myResponseReceiverThread.join(3000);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "detected an exception during waiting for ending of thread. The thread id = " + myResponseReceiverThread.getId());
                    }
                    
                    if (myResponseReceiverThread.getState() != Thread.State.TERMINATED)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.StopThreadFailure + myResponseReceiverThread.getId());

                        try
                        {
                            myResponseReceiverThread.stop();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.AbortThreadFailure, err);
                        }
                    }
                }
                myResponseReceiverThread = null;

                myResponseMessageHandler = null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        if (myResponseMessageHandler != null)
        {
            return myIsListeningToResponses;
        }

        return myTcpClient != null;
    }

    
    @Override
    public boolean isStreamWritter()
    {
        return false;
    }

    @Override
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOpenConnectionLock)
            {
                byte[] aMessage = (byte[])message;
                OutputStream aSenderStream = myTcpClient.getOutputStream();
                aSenderStream.write(aMessage, 0, aMessage.length);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void sendMessage(IMethod1<OutputStream> toStreamWritter)
            throws Exception
    {
        throw new UnsupportedOperationException("toStreamWritter is not supported.");
    }
    
    private void doResponseListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myIsListeningToResponses = true;
            myListeningToResponsesStartedEvent.set();

            try
            {
                InputStream anInputStream = myTcpClient.getInputStream();
                MessageContext aContext = new MessageContext(anInputStream, myIpAddress, null);

                while (!myStopReceivingRequestedFlag)
                {
                    if (!myResponseMessageHandler.invoke(aContext))
                    {
                        // Disconnected.
                        break;
                    }
                }
            }
            catch (Exception err)
            {
                // If it is not an exception caused by closing the socket.
                if (!myStopReceivingRequestedFlag)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
                }
            }

            myIsListeningToResponses = false;
            myListeningToResponsesStartedEvent.reset();

            // If this closing is not caused by CloseConnection method.
            if (!myStopReceivingRequestedFlag)
            {
                // Try to clean the connection.
                ThreadPool.queueUserWorkItem(myCloseConnectionCallback);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private InetSocketAddress mySocketAddress;
    private Socket myTcpClient;
    private IClientSecurityFactory myClientSecurityFactory;
    private String myIpAddress;
    private Object myOpenConnectionLock = new Object();

    private IFunction1<Boolean, MessageContext> myResponseMessageHandler;
    private Thread myResponseReceiverThread;
    private volatile boolean myStopReceivingRequestedFlag;
    private volatile boolean myIsListeningToResponses;
    private ManualResetEvent myListeningToResponsesStartedEvent = new ManualResetEvent(false);
    
    private Runnable myCloseConnectionCallback = new Runnable()
    {
        @Override
        public void run()
        {
            closeConnection();
        }
    };
    
    private Runnable myDoResponseListening = new Runnable()
    {
        @Override
        public void run()
        {
            doResponseListening();
        }
    };

    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + ' ';
    }
}

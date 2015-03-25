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
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.IpAddressUtil;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.OutputStreamTimeoutWriter;
import eneter.net.system.*;
import eneter.net.system.threading.internal.*;


class TcpOutputConnector implements IOutputConnector
{
    public TcpOutputConnector(String ipAddressAndPort, IClientSecurityFactory clientSecurityFactory)
            throws Exception
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
                
                if (myResponseReceiverThread != null && Thread.currentThread().getId() != myResponseReceiverThread.getId())
                {
                    if (myResponseReceiverThread.getState() != Thread.State.NEW)
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
                            EneterTrace.warning(TracedObject() + ErrorHandler.FailedToStopThreadId + myResponseReceiverThread.getId());
    
                            try
                            {
                                myResponseReceiverThread.stop();
                            }
                            catch (Exception err)
                            {
                                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToAbortThread, err);
                            }
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
    public void sendRequestMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOpenConnectionLock)
            {
                OutputStream aStream = myTcpClient.getOutputStream();
                int aSendTimeout = myClientSecurityFactory.getSendTimeout();
                byte[] anEncodedMessage = (byte[])myProtocolFormatter.encodeMessage(myOutputConnectorAddress, message);
                myStreamWriter.write(aStream, anEncodedMessage, aSendTimeout);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
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
                while (!myStopReceivingRequestedFlag)
                {
                    InputStream anInputStream = myTcpClient.getInputStream();
                    ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage((InputStream)anInputStream);
                    if (aProtocolMessage == null)
                    {
                        // The client is disconneced by the service.
                        break;
                    }
                    
                    MessageContext aMessageContext = new MessageContext(aProtocolMessage, myIpAddress);
                    
                    try
                    {
                        myResponseMessageHandler.invoke(aMessageContext);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    }
                }
            }
            catch (Exception err)
            {
                // If it is not an exception caused by closing the socket.
                if (!myStopReceivingRequestedFlag)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedInListeningLoop, err);
                }
            }

            myIsListeningToResponses = false;
            myListeningToResponsesStartedEvent.reset();

            // If the connection was closed from the service.
            if (!myStopReceivingRequestedFlag)
            {
                IMethod1<MessageContext> aResponseHandler = myResponseMessageHandler;
                closeConnection();

                try
                {
                    aResponseHandler.invoke(null);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    
    private InetSocketAddress mySocketAddress;
    private String myOutputConnectorAddress;
    private Socket myTcpClient;
    private IClientSecurityFactory myClientSecurityFactory;
    private IProtocolFormatter myProtocolFormatter;
    private String myIpAddress;
    private Object myOpenConnectionLock = new Object();

    private IMethod1<MessageContext> myResponseMessageHandler;
    private Thread myResponseReceiverThread;
    private volatile boolean myStopReceivingRequestedFlag;
    private volatile boolean myIsListeningToResponses;
    private ManualResetEvent myListeningToResponsesStartedEvent = new ManualResetEvent(false);
    
    private OutputStreamTimeoutWriter myStreamWriter = new OutputStreamTimeoutWriter();
    
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

    @Override
    public void openConnection(IMethod1<MessageContext> responseMessageHandler)
            throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.*;
import java.net.Socket;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.*;
import eneter.net.system.*;
import eneter.net.system.internal.IDisposable;


class TcpInputConnector implements IInputConnector
{
    private class ResponseSender implements ISender, IDisposable
    {
        public ResponseSender(OutputStream clientStream, int sendTimeout)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myClientStream = clientStream;
                mySendTimeout = sendTimeout;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        //@Override
        public void dispose()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                if (myClientStream != null)
                {
                    myClientStream.close();
                }
            }
            catch (IOException err)
            {
                EneterTrace.error(getClass().getSimpleName() + " failed to close the client socket.", err);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
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
                synchronized (mySenderLock)
                {
                    byte[] aMessage = (byte[])message;
                    myStreamWriter.write(myClientStream, aMessage, mySendTimeout);
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
            throw new UnsupportedOperationException("Sending via the stream is not supported.");
        }
        
        private OutputStream myClientStream;
        private int mySendTimeout;
        private Object mySenderLock = new Object();
        private OutputStreamTimeoutWriter myStreamWriter = new OutputStreamTimeoutWriter();
    }

    
    
    public TcpInputConnector(String ipAddressAndPort, IServerSecurityFactory securityFactory)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myTcpListenerProvider = new TcpListenerProvider(ipAddressAndPort, securityFactory);
            mySecurityFactory = securityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public void startListening(
            IFunction1<Boolean, MessageContext> messageHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myMessageHandler = messageHandler;
                myTcpListenerProvider.startListening(myHandleConnection);
            }
            catch (Exception err)
            {
                myMessageHandler = null;
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myTcpListenerProvider.stopListening();
            myMessageHandler = null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening()
    {
        return myTcpListenerProvider.isListening();
    }

    @Override
    public ISender createResponseSender(String responseReceiverAddress)
    {
        throw new UnsupportedOperationException("CreateResponseSender is not supported in TcpServiceConnector.");
    }

    private void handleConnection(Socket clientSocket) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aClientIp = IpAddressUtil.getRemoteIpAddress(clientSocket);

            InputStream anInputStream = clientSocket.getInputStream();
            OutputStream anOutputStream = clientSocket.getOutputStream();
            
            int aSendTimeout = mySecurityFactory.getSendTimeout();
            ResponseSender aResponseSender = new ResponseSender(anOutputStream, aSendTimeout);
            MessageContext aMessageContext = new MessageContext(anInputStream, aClientIp, aResponseSender);

            // While the stop of listening is not requested and the connection is not closed.
            boolean aConnectionIsOpen = true;
            while (aConnectionIsOpen)
            {
                aConnectionIsOpen = myMessageHandler.invoke(aMessageContext);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private TcpListenerProvider myTcpListenerProvider;
    private IServerSecurityFactory mySecurityFactory;
    private IFunction1<Boolean, MessageContext> myMessageHandler;
    
    private IMethod1<Socket> myHandleConnection = new IMethod1<Socket>()
    {
        @Override
        public void invoke(Socket t) throws Exception
        {
            handleConnection(t);
        }
    };
    
}
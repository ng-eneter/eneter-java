/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.internal.*;
import eneter.net.system.*;
import eneter.net.system.internal.IDisposable;
import eneter.net.system.threading.internal.ManualResetEvent;
import eneter.net.system.threading.internal.ThreadPool;


class TcpInputConnector implements IInputConnector
{
    private class ResponseSender implements ISender, IDisposable
    {
        // This is the helper class allowing to send a response message using the send timeout.
        // (because java socket does not have the sending timeout)
        private class TcpResponseMessageSender implements Runnable
        {
            public void send(byte[] messageToSend) throws Exception
            {
                // Prepare sending.
                myMessage = messageToSend;
                mySendException = null;
                mySendCompletedEvent.reset();
                
                // Start sending in another thread.
                ThreadPool.queueUserWorkItem(this);
                
                // Wait until sending is completed.
                if (!mySendCompletedEvent.waitOne(mySendTimeout))
                {
                    throw new TimeoutException("ResponseSender failed to send the message within specified timeout: " + Integer.toString(mySendTimeout) + "ms.");
                }
                
                if (mySendException != null)
                {
                    throw mySendException;
                }
            }
            

            @Override
            public void run()
            {
                try
                {
                    myClientStream.write(myMessage, 0, myMessage.length);
                }
                catch (Exception err)
                {
                    mySendException = err;
                }
                finally
                {
                    mySendCompletedEvent.set();
                }
            }
            
            private ManualResetEvent mySendCompletedEvent = new ManualResetEvent(false);
            private byte[] myMessage;
            private Exception mySendException;
        }
        
        
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
                    myResponseSender.send(aMessage);
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
        private TcpResponseMessageSender myResponseSender = new TcpResponseMessageSender();
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

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.IOException;
import java.net.*;

import eneter.messaging.dataprocessing.messagequeueing.WorkingThread;
import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;
import eneter.net.system.*;


public abstract class TcpInputChannelBase
{
    public TcpInputChannelBase(String ipAddressAndPort, IListenerProvider tcpListenerProvider, IServerSecurityFactory serverSecurityFactory) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(ipAddressAndPort))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }
            
            myTcpListenerProvider = tcpListenerProvider;
            
            myChannelId = ipAddressAndPort;
            myMessageProcessingThread = new WorkingThread<ProtocolMessage>(ipAddressAndPort);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public String getChannelId()
    {
        return myChannelId;
    }
    

    public void startListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }
                
                try
                {
                    // Start the working thread for removing messages from the queue
                    myMessageProcessingThread.registerMessageHandler(myMessageHandlerHandler);
                    
                    // Start TCP listener.
                    myTcpListenerProvider.startListening(myHandleConnection);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);

                    try
                    {
                        // Clear after failed start
                        stopListening();
                    }
                    catch (Exception err2)
                    {
                        // We tried to clean after failure. The exception can be ignored.
                    }

                    throw err;
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);

                    try
                    {
                        // Clear after failed start
                        stopListening();
                    }
                    catch (Exception err2)
                    {
                        // We tried to clean after failure. The exception can be ignored.
                    }

                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                // Try to stop connections with clients.
                try
                {
                    disconnectClients();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to close Tcp connections with clients.", err);
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + "failed to close Tcp connections with clients.", err);
                    throw err;
                }
                
                // Stop the TCP listener.
                myTcpListenerProvider.stopListening();

                // Stop thread processing the queue with messages.
                try
                {
                    myMessageProcessingThread.unregisterMessageHandler();
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.UnregisterMessageHandlerThreadFailure, err);
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.UnregisterMessageHandlerThreadFailure, err);
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public boolean isListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                return myTcpListenerProvider.isListening();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    protected abstract void disconnectClients() throws IOException;
    
    protected abstract void handleConnection(Socket clientSocket) throws Exception;
    
    protected abstract void handleMessage(ProtocolMessage message);
    

    protected String myChannelId = "";
   
   
    protected Object myListeningManipulatorLock = new Object();
    private IListenerProvider myTcpListenerProvider;
    protected WorkingThread<ProtocolMessage> myMessageProcessingThread;
    
    
    private IMethod1<Socket> myHandleConnection = new IMethod1<Socket>()
    {
        @Override
        public void invoke(Socket x) throws Exception
        {
            handleConnection(x);
        }
    };
    
   
    private IMethod1<ProtocolMessage> myMessageHandlerHandler = new IMethod1<ProtocolMessage>()
    {
        @Override
        public void invoke(ProtocolMessage message) throws Exception
        {
            handleMessage(message);
        }
    };
    
    protected abstract String TracedObject();
}

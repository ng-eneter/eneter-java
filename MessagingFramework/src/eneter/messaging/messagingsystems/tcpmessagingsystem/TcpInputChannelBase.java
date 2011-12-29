package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.IOException;
import java.net.*;

import eneter.messaging.dataprocessing.messagequeueing.WorkingThread;
import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.ProtocolMessage;
import eneter.net.system.*;
import eneter.net.system.threading.ManualResetEvent;
import eneter.net.system.threading.ThreadPool;

public abstract class TcpInputChannelBase
{
    public TcpInputChannelBase(String ipAddressAndPort) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(ipAddressAndPort))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }
            
            try
            {
                myUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            
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
                    myStopTcpListeningRequested = false;
                    
                    // Start the working thread for removing messages from the queue
                    myMessageProcessingThread.registerMessageHandler(myMessageHandlerHandler);
                    
                    myServerSocket = new ServerSocket(myUri.getPort(), 1000, InetAddress.getByName(myUri.getHost()));
                    
                    // Listen in another thread.
                    myTcpListeningThread = new Thread(myDoTcpListeningRunnable);
                    myTcpListeningThread.start();
                    
                    // Wait until the thread really started the listening.
                    if (!myListeningStartedEvent.waitOne(5000))
                    {
                        throw new IllegalStateException("The thread listening to messages did not start.");
                    }
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
    
    @SuppressWarnings("deprecation")
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                myStopTcpListeningRequested = true;

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

                if (myServerSocket != null)
                {
                    try
                    {
                        myServerSocket.close();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.StopListeningFailure, err);
                    }
                    myServerSocket = null;
                }

                if (myTcpListeningThread != null && myTcpListeningThread.getState() != Thread.State.NEW)
                {
                    try
                    {
                        myTcpListeningThread.join(1000);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "detected an exception during waiting for ending of thread. The thread id = " + myTcpListeningThread.getId());
                    }
                    
                    if (myTcpListeningThread.getState() != Thread.State.TERMINATED)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.StopThreadFailure + myTcpListeningThread.getId());

                        try
                        {
                            // The thread must be stopped
                            myTcpListeningThread.stop();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.AbortThreadFailure, err);
                        }
                        catch (Error err)
                        {
                            EneterTrace.error(TracedObject() + ErrorHandler.AbortThreadFailure, err);
                            throw err;
                        }
                    }
                }
                myTcpListeningThread = null;

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
    
    
    public boolean isListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                return myServerSocket != null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private void doTcpListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myListeningStartedEvent.set();
            
            try
            {
                // Listening loop.
                while (!myStopTcpListeningRequested)
                {
                    // Wait while the client is connected.
                    final Socket aClientSocket = myServerSocket.accept();
                    
                    // Process the incoming connection in a different thread.
                    ThreadPool.queueUserWorkItem(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            handleConnection(aClientSocket);
                        }
                    });
                }
            }
            catch (SocketException err)
            {
                // If the server is not listening, then this exception occurred because the listening
                // was stopped and tracing is not desired.
                // If the server still listens, then there is some other problem that must be traced.
                if (myServerSocket != null)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.DoListeningFailure, err);
            }
            
            myListeningStartedEvent.reset();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    protected abstract void disconnectClients() throws IOException;
    
    protected abstract void handleConnection(Socket clientSocket);
    
    protected abstract void messageHandler(ProtocolMessage message);
    
    private URI myUri;
    protected String myChannelId = "";
    
    private ServerSocket myServerSocket;
    private Thread myTcpListeningThread;
    protected volatile boolean myStopTcpListeningRequested;
    private ManualResetEvent myListeningStartedEvent = new ManualResetEvent(false);
    
    protected WorkingThread<ProtocolMessage> myMessageProcessingThread;
    
    protected Object myListeningManipulatorLock = new Object();
    
    
    private Runnable myDoTcpListeningRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            doTcpListening();
        }
    };
    
    private IMethod1<ProtocolMessage> myMessageHandlerHandler = new IMethod1<ProtocolMessage>()
    {
        @Override
        public void invoke(ProtocolMessage message) throws Exception
        {
            messageHandler(message);
        }
    };
    
    protected abstract String TracedObject();
}

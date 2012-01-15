package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.ErrorHandler;
import eneter.net.system.IMethod1;
import eneter.net.system.threading.ManualResetEvent;
import eneter.net.system.threading.ThreadPool;

class TcpListenerProvider
{
    public TcpListenerProvider(InetAddress address, int port)
    {
        this(address, port, new NoneSecurityServerFactory());
    }
    
    public TcpListenerProvider(InetAddress address, int port, IServerSecurityFactory serverSecurityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAddress = address;
            myPort = port;
            myServerSecurityFactory = serverSecurityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void startListening(IMethod1<Socket> connectionHandler) throws Exception
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
                    
                    myConnectionHandler = connectionHandler;
                    
                    //myServerSocket = new ServerSocket(myUri.getPort(), 1000, InetAddress.getByName(myUri.getHost()));
                    myServerSocket = myServerSecurityFactory.createServerSocket(myAddress, myPort);
                    
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
                            processConnection(aClientSocket);
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
    
    private void processConnection(Socket tcpClient)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // Call user provided connection handler.
                myConnectionHandler.invoke(tcpClient);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ProcessingTcpConnectionFailure, err);
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.ProcessingTcpConnectionFailure, err);
                throw err;
            }
            finally
            {
                if (tcpClient != null)
                {
                    try
                    {
                        tcpClient.close();
                    }
                    catch (IOException e)
                    {
                        EneterTrace.warning(TracedObject() + "failed to close the client socket.");
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private InetAddress myAddress;
    private int myPort;
    private IServerSecurityFactory myServerSecurityFactory;
    private IMethod1<Socket> myConnectionHandler;
    
    
    private ServerSocket myServerSocket;
    private Thread myTcpListeningThread;
    private volatile boolean myStopTcpListeningRequested;
    private ManualResetEvent myListeningStartedEvent = new ManualResetEvent(false);
    
    private Object myListeningManipulatorLock = new Object();
    
    
    private Runnable myDoTcpListeningRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            doTcpListening();
        }
    };
    
    
    private String TracedObject()
    {
        return "TcpListenerProvider ";
    }
}

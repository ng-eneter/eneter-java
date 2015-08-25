/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.messaging.threading.dispatching.internal.SyncDispatcher;
import eneter.net.system.internal.IMethod2;
import eneter.net.system.threading.internal.ManualResetEvent;

class UdpReceiver
{
    public UdpReceiver(InetSocketAddress serviceEndPoint, boolean isService)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myServiceEndpoint = serviceEndPoint;
            myIsService = isService;
            myWorkingThreadDispatcher = new SyncDispatcher();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void startListening(IMethod2<byte[], InetSocketAddress> messageHandler) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myListeningManipulatorLock.lock();
            try
            {
                if (isListening())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyListening;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                if (messageHandler == null)
                {
                    throw new IllegalArgumentException("The input parameter messageHandler is null.");
                }

                try
                {
                    myStopListeningRequested = false;
                    myMessageHandler = messageHandler;

                    // Create unbound socket.
                    mySocket = new DatagramSocket(null);
                            
                    // Note: bigger buffer increases the chance the datagram is not lost.
                    mySocket.setReceiveBufferSize(1048576);
                    if (myIsService)
                    {
                        mySocket.bind(myServiceEndpoint);
                    }
                    else
                    {
                        mySocket.connect(myServiceEndpoint);
                    }

                    myListeningThread = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            doListening();
                        }
                    }, (myIsService) ? "Eneter.UdpServiceListener" : "Eneter.UdpClientListener");
                    myListeningThread.start();

                    // Wait until the listening thread is ready.
                    myListeningToResponsesStartedEvent.waitOne(5000);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToStartListening, err);

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
            finally
            {
                myListeningManipulatorLock.unlock();
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
            myListeningManipulatorLock.lock();
            try
            {
                // Stop the thread listening to messages from the shared memory.
                myStopListeningRequested = true;

                // Note: this receiver needs to close the socket here
                //       because it will release the waiting in the listener thread.
                if (mySocket != null)
                {
                    mySocket.close();
                    mySocket = null;
                }

                if (myListeningThread != null && myListeningThread.getState() != Thread.State.NEW)
                {
                    try
                    {
                        myListeningThread.join(1000);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "detected an exception during waiting for ending of thread. The thread id = " + myListeningThread.getId());
                    }
                    
                    if (myListeningThread.getState() != Thread.State.TERMINATED)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.FailedToStopThreadId + myListeningThread.getId());

                        try
                        {
                            // The thread must be stopped
                            myListeningThread.stop();
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.FailedToAbortThread, err);
                        }
                    }
                }
                myListeningThread = null;

                myMessageHandler = null;
            }
            finally
            {
                myListeningManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public boolean isListening()
    {
        myListeningManipulatorLock.lock();
        try
        {
            return myIsListening;
        }
        finally
        {
            myListeningManipulatorLock.unlock();
        }
    }
    
    public DatagramSocket getUdpSocket()
    { 
        return mySocket;
    }
    
    private void doListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                byte[] aBuf = new byte[65536];
                DatagramPacket aPacket = new DatagramPacket(aBuf, aBuf.length);

                myIsListening = true;
                myListeningToResponsesStartedEvent.set();

                // Loop until the stop is requested.
                while (!myStopListeningRequested)
                {
                    // Wait for a message.
                    mySocket.receive(aPacket);
                    
                    final InetSocketAddress aSenderEndPoint = (InetSocketAddress)aPacket.getSocketAddress();
                    final byte[] aDatagram = new byte[aPacket.getLength()];
                    System.arraycopy(aPacket.getData(), 0, aDatagram, 0, aDatagram.length);
                    
                    myWorkingThreadDispatcher.invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                myMessageHandler.invoke(aDatagram, aSenderEndPoint);
                            }
                            catch (Exception err)
                            {
                                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                            }
                        }
                    });
                            
                }
            }
            catch (Exception err)
            {
                // If the error did not occur because of StopListening().
                if (!myStopListeningRequested)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedInListeningLoop, err);
                }
            }

            // If the listening got interrupted.
            if (!myStopListeningRequested)
            {
                try
                {
                    myWorkingThreadDispatcher.invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                myMessageHandler.invoke(null, null);
                            }
                            catch (Exception err)
                            {
                                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                            }
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }

            myIsListening = false;
            myListeningToResponsesStartedEvent.reset();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private boolean myIsService;
    private SocketAddress myServiceEndpoint;
    private DatagramSocket mySocket;
    private volatile boolean myIsListening;
    private volatile boolean myStopListeningRequested;
    private ThreadLock myListeningManipulatorLock = new ThreadLock();
    private Thread myListeningThread;
    private ManualResetEvent myListeningToResponsesStartedEvent = new ManualResetEvent(false);
    private IMethod2<byte[], InetSocketAddress> myMessageHandler;
    private IThreadDispatcher myWorkingThreadDispatcher;
    
    private String TracedObject()
    {
        return (myIsService) ? "UdpReceiver (request receiver) " : "UdpReceiver (response receiver) ";
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

import java.util.concurrent.TimeoutException;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.internal.*;
import eneter.net.system.threading.internal.*;

class AuthenticatedDuplexOutputChannel implements IDuplexOutputChannel
{

    @Override
    public Event<DuplexChannelMessageEventArgs> responseMessageReceived()
    {
        return myResponseMessageReceivedEventImpl.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpenedEventImpl.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosedEventImpl.getApi();
    }
    
    
    
    public AuthenticatedDuplexOutputChannel(IDuplexOutputChannel underlyingOutputChannel,
            IGetLoginMessage getLoginMessageCallback,
            IGetHandshakeResponseMessage getHandshakeResponseMessageCallback,
            long authenticationTimeout,
            IThreadDispatcher threadDispatcher)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingOutputChannel = underlyingOutputChannel;
            myGetLoginMessageCallback = getLoginMessageCallback;
            myGetHandshakeResponseMessageCallback = getHandshakeResponseMessageCallback;
            myAuthenticationTimeout = authenticationTimeout;

            myUnderlyingOutputChannel.connectionClosed().subscribe(myOnConnectionClosed);
            myUnderlyingOutputChannel.responseMessageReceived().subscribe(myOnResponseMessageReceived);
            
            myThreadDispatcher = threadDispatcher;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public String getChannelId()
    {
        return myUnderlyingOutputChannel.getChannelId();
    }

    @Override
    public String getResponseReceiverId()
    {
        return myUnderlyingOutputChannel.getResponseReceiverId();
    }

    @Override
    public void openConnection() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                if (isConnected())
                {
                    String aMessage = TracedObject() + ErrorHandler.IsAlreadyConnected;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                try
                {
                    // Reset internal states.
                    myIsHandshakeResponseSent = false;
                    myIsConnectionAcknowledged = false;
                    myAuthenticationEnded.reset();

                    myUnderlyingOutputChannel.openConnection();

                    // Send the login message.
                    Object aLoginMessage;
                    try
                    {
                        aLoginMessage = myGetLoginMessageCallback.getLoginMessage(getChannelId(), getResponseReceiverId());
                    }
                    catch (Exception err)
                    {
                        String anErrorMessage = TracedObject() + "failed to get the login message.";
                        EneterTrace.error(anErrorMessage, err);
                        throw err;
                    }

                    try
                    {
                        myUnderlyingOutputChannel.sendMessage(aLoginMessage);
                    }
                    catch (Exception err)
                    {
                        String anErrorMessage = TracedObject() + "failed to send the login message.";
                        EneterTrace.error(anErrorMessage, err);
                        throw err;
                    }

                    // Wait until the handshake is completed.
                    if (!myAuthenticationEnded.waitOne(myAuthenticationTimeout))
                    {
                        String anErrorMessage = TracedObject() + "failed to process authentication within defined timeout " + Long.toString(myAuthenticationTimeout) + " ms.";
                        EneterTrace.error(anErrorMessage);
                        throw new TimeoutException(anErrorMessage);
                    }
                    
                    if (!isConnected())
                    {
                        String anErrorMessage = TracedObject() + "failed to authenticate '" + aLoginMessage + "'.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }
                }
                catch (Exception err)
                {
                    closeConnection();
                    throw err;
                }
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
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
            myConnectionManipulatorLock.lock();
            try
            {
                myUnderlyingOutputChannel.closeConnection();
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
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
        myConnectionManipulatorLock.lock();
        try
        {
            return myIsConnectionAcknowledged && myUnderlyingOutputChannel.isConnected();
        }
        finally
        {
            myConnectionManipulatorLock.unlock();
        }
    }
    
    @Override
    public IThreadDispatcher getDispatcher()
    {
        return myUnderlyingOutputChannel.getDispatcher();
    }

    @Override
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myConnectionManipulatorLock.lock();
            try
            {
                if (!isConnected())
                {
                    String aMessage = TracedObject() + ErrorHandler.FailedToSendMessageBecauseNotConnected;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                myUnderlyingOutputChannel.sendMessage(message);
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private void onConnectionClosed(Object sender, final DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // If there is waiting for connection open release it.
            myAuthenticationEnded.set();
            
            // If the connection was authenticated then notify that it was closed.
            if (myIsConnectionAcknowledged)
            {
                myThreadDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyEvent(myConnectionClosedEventImpl, e, false);
                    }
                });
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    // Note: This method is called in the thread defined in used ThreadDispatcher.
    //       So it is the "correct" thread.
    private void onResponseMessageReceived(Object sender, final DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myIsConnectionAcknowledged)
            {
                // If the connection is properly established via handshaking.
                myThreadDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyEvent(myResponseMessageReceivedEventImpl, e, true);
                    }
                });
                
                return;
            }
            
            boolean aCloseConnectionFlag = false;
            
            // This is the handshake message.
            if (!myIsHandshakeResponseSent)
            {
                EneterTrace.debug("HANDSHAKE RECEIVED");

                // Get the response for the handshake message.
                Object aHandshakeResponseMessage = null;
                try
                {
                    aHandshakeResponseMessage = myGetHandshakeResponseMessageCallback.getHandshakeResponseMessage(e.getChannelId(), e.getResponseReceiverId(), e.getMessage());
                    aCloseConnectionFlag = (aHandshakeResponseMessage == null);
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + "failed to get the handshake response message. The connection will be closed.";
                    EneterTrace.error(anErrorMessage, err);
                    
                    aCloseConnectionFlag = true;
                }

                // Send back the response for the handshake.
                if (!aCloseConnectionFlag)
                {
                    try
                    {
                        // Note: keep setting this flag before sending. Otherwise synchronous messaging will not work!
                        myIsHandshakeResponseSent = true;
                        myUnderlyingOutputChannel.sendMessage(aHandshakeResponseMessage);
                    }
                    catch (Exception err)
                    {
                        myIsHandshakeResponseSent = false;
    
                        String anErrorMessage = TracedObject() + "failed to send the handshake response message. The connection will be closed.";
                        EneterTrace.error(anErrorMessage, err);
    
                        aCloseConnectionFlag = true;
                    }
                }
            }
            else
            {
                EneterTrace.debug("CONNECTION ACKNOWLEDGE RECEIVED");
                
                // If the handshake was sent then this message must be acknowledgement.
                String anAcknowledgeMessage = Cast.as(e.getMessage(), String.class);

                // If the acknowledge message is wrong then disconnect.
                if (StringExt.isNullOrEmpty(anAcknowledgeMessage) || !anAcknowledgeMessage.equals("OK"))
                {
                    String anErrorMessage = TracedObject() + "detected incorrect acknowledge message. The connection will be closed.";
                    EneterTrace.error(anErrorMessage);

                    aCloseConnectionFlag = true;
                }
                else
                {
                    myIsConnectionAcknowledged = true;
                    myAuthenticationEnded.set();
    
                    // Notify the connection is open.
                    final DuplexChannelEventArgs anEventArgs = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId(), e.getSenderAddress());
                    myThreadDispatcher.invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            notifyEvent(myConnectionOpenedEventImpl, anEventArgs, false);
                        }
                    });
                }
            }
            
            if (aCloseConnectionFlag)
            {
                myUnderlyingOutputChannel.closeConnection();
                
                // Release the waiting in OpenConnection(..).
                myAuthenticationEnded.set();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private <T> void notifyEvent(EventImpl<T> handler, T event, boolean isNobodySubscribedWarning)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler != null)
            {
                try
                {
                    handler.raise(this, event);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
            else if (isNobodySubscribedWarning)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IThreadDispatcher myThreadDispatcher;
    private IDuplexOutputChannel myUnderlyingOutputChannel;
    private IGetLoginMessage myGetLoginMessageCallback;
    private IGetHandshakeResponseMessage myGetHandshakeResponseMessageCallback;
    private long myAuthenticationTimeout;
    private boolean myIsHandshakeResponseSent;
    private boolean myIsConnectionAcknowledged;
    private ManualResetEvent myAuthenticationEnded = new ManualResetEvent(false);
    private ThreadLock myConnectionManipulatorLock = new ThreadLock();
    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionClosed = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionClosed(sender, e);
        }
    };
     
    private EventHandler<DuplexChannelMessageEventArgs> myOnResponseMessageReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onResponseMessageReceived(sender, e);
        }
    };
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " '" + getChannelId() + "' ";
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.util.UUID;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.diagnostic.internal.ThreadLock;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.internal.*;


public class DefaultDuplexOutputChannel implements IDuplexOutputChannel
{
    @Override
    public Event<DuplexChannelMessageEventArgs> responseMessageReceived()
    {
        return myResponseMessageReceived.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpened.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosed.getApi();
    }
    
    
    public DefaultDuplexOutputChannel(String channelId, String responseReceiverId,
            IThreadDispatcher eventDispatcher,
            IThreadDispatcher dispatcherAfterResponseReading,
            IOutputConnectorFactory outputConnectorFactory) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                if (StringExt.isNullOrEmpty(channelId))
                {
                    EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                    throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
                }

                myChannelId = channelId;
                
                if (StringExt.isNullOrEmpty(responseReceiverId))
                {
                    myResponseReceiverId = channelId + "_" + UUID.randomUUID().toString();
                }
                else if (responseReceiverId.equals("*"))
                {
                    String anErrorMessage = "responseReceiverId cannot be '*'. It is reserved for broadcast response messages.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalArgumentException(anErrorMessage);
                }
                else
                {
                    myResponseReceiverId = responseReceiverId;
                }

                myDispatcher = eventDispatcher;
                
                // Internal dispatcher used when the message is decoded.
                // E.g. Shared memory mesaging needs to return looping immediately the protocol message is decoded
                //      so that other senders are not blocked.
                myDispatchingAfterResponseReading = dispatcherAfterResponseReading;
                
                myOutputConnector = outputConnectorFactory.createOutputConnector(myChannelId, myResponseReceiverId);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
    

    @Override
    public String getChannelId()
    {
        return myChannelId;
    }

    @Override
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
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
                    myConnectionIsCorrectlyOpen = true;
                    
                    // Connect and start listening to response messages.
                    myOutputConnector.openConnection(myHandleResponse);
                }
                catch (Exception err)
                {
                    myConnectionIsCorrectlyOpen = false;
                    
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToOpenConnection, err);

                    try
                    {
                        closeConnection();
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
                myConnectionManipulatorLock.unlock();
            }
            
            // Invoke the event notifying, the connection was opened.
            myDispatcher.invoke(new Runnable()
            {
                @Override
                public void run()
                {
                    notifyEvent(myConnectionOpened);
                }
            });
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
            cleanAfterConnection(true, false);
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
            return (myOutputConnector.isConnected());
        }
        finally
        {
            myConnectionManipulatorLock.unlock();
        }
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

                try
                {
                    // Send the message.
                    myOutputConnector.sendRequestMessage(message);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.FailedToSendMessage, err);
                    cleanAfterConnection(true, true);
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
    public IThreadDispatcher getDispatcher()
    {
        return myDispatcher;
    }
    
    private void handleResponse(final MessageContext messageContext) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (messageContext == null ||
                messageContext.getProtocolMessage() == null ||
                messageContext.getProtocolMessage().MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                EneterTrace.debug("CLIENT DISCONNECTED RECEIVED");
                myDispatchingAfterResponseReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        cleanAfterConnection(false, true);
                    }
                });
            }
            else if (messageContext.getProtocolMessage().MessageType == EProtocolMessageType.MessageReceived)
            {
                EneterTrace.debug("RESPONSE MESSAGE RECEIVED");
                myDispatchingAfterResponseReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        myDispatcher.invoke(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                notifyResponseMessageReceived(messageContext.getProtocolMessage().Message);
                            }
                        });
                    }
                });
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.FailedToReceiveMessageBecauseIncorrectFormat);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void cleanAfterConnection(boolean sendCloseMessageFlag, boolean notifyConnectionClosedFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            boolean aConnectionWasCorrectlyOpen = false;
            
            myConnectionManipulatorLock.lock();
            try
            {
                if (sendCloseMessageFlag)
                {
                    try
                    {
                        myOutputConnector.closeConnection();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.FailedToCloseConnection, err);
                    }
                }

                // Note: the notification must run outside the lock because of potententional deadlock.
                aConnectionWasCorrectlyOpen = myConnectionIsCorrectlyOpen;
                myConnectionIsCorrectlyOpen = false;
            }
            finally
            {
                myConnectionManipulatorLock.unlock();
            }
        
            // Notify the connection closed only if it was successfully open before.
            // E.g. It will be not notified if the CloseConnection() is called for already closed connection.
            if (aConnectionWasCorrectlyOpen && notifyConnectionClosedFlag)
            {
                myDispatcher.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyEvent(myConnectionClosed);
                    }
                });
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyResponseMessageReceived(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseMessageReceived.isSubscribed())
            {
                try
                {
                    myResponseMessageReceived.raise(this, new DuplexChannelMessageEventArgs(myChannelId, message, myResponseReceiverId, ""));
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyEvent(final EventImpl<DuplexChannelEventArgs> eventHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                if (eventHandler.isSubscribed())
                {
                    DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId(), "");
                    eventHandler.raise(this, aMsg);
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IThreadDispatcher myDispatchingAfterResponseReading;
    private IOutputConnector myOutputConnector;
    private boolean myConnectionIsCorrectlyOpen;
    private ThreadLock myConnectionManipulatorLock = new ThreadLock();
    
    private String myChannelId;
    private String myResponseReceiverId;
    private IThreadDispatcher myDispatcher;
    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceived = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOpened = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosed = new EventImpl<DuplexChannelEventArgs>();
    
    
    private IMethod1<MessageContext> myHandleResponse = new IMethod1<MessageContext>()
    {
        @Override
        public void invoke(MessageContext x) throws Exception
        {
            handleResponse(x);
        }
    };
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " '" + myChannelId + "' ";
    }
}

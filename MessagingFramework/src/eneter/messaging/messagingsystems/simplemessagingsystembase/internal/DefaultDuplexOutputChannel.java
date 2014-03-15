/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.io.InputStream;
import java.util.UUID;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
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
            IOutputConnectorFactory outputConnectorFactory,
            IProtocolFormatter<?> protocolFormatter,
            boolean startReceiverAfterSendOpenRequest) throws Exception
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
                myResponseReceiverId = (StringExt.isNullOrEmpty(responseReceiverId)) ? channelId + "_" + UUID.randomUUID().toString() : responseReceiverId;

                myProtocolFormatter = protocolFormatter;
                
                myDispatcher = eventDispatcher;
                
                // Internal dispatcher used when the message is decoded.
                // E.g. Shared memory meesaging needs to return looping immediately the protocol message is decoded
                //      so that other senders are not blocked.
                myDispatchingAfterResponseReading = dispatcherAfterResponseReading;
                
                myOutputConnector = outputConnectorFactory.createOutputConnector(myChannelId, myResponseReceiverId);
                myStartReceiverAfterSendOpenRequest = startReceiverAfterSendOpenRequest;
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
            synchronized (myConnectionManipulatorLock)
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
                    
                    if (!myStartReceiverAfterSendOpenRequest)
                    {
                        // Connect and start listening to response messages.
                        myOutputConnector.openConnection(myResponseHandler);
                    }

                    // Send the open connection request.
                    SenderUtil.SendOpenConnection(myOutputConnector, getResponseReceiverId(), myProtocolFormatter);

                    if (myStartReceiverAfterSendOpenRequest)
                    {
                        // Connect and start listening to response messages.
                        myOutputConnector.openConnection(myResponseHandler);
                    }
                }
                catch (Exception err)
                {
                    myConnectionIsCorrectlyOpen = false;
                    
                    EneterTrace.error(TracedObject() + ErrorHandler.OpenConnectionFailure, err);

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
            cleanAfterConnection(true);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        synchronized (myConnectionManipulatorLock)
        {
            return (myOutputConnector.isConnected());
        }
    }

    @Override
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                if (!isConnected())
                {
                    String aMessage = TracedObject() + ErrorHandler.SendMessageNotConnectedFailure;
                    EneterTrace.error(aMessage);
                    throw new IllegalStateException(aMessage);
                }

                try
                {
                    // Send the message.
                    SenderUtil.sendMessage(myOutputConnector, getResponseReceiverId(), message, myProtocolFormatter);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
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
    public IThreadDispatcher getDispatcher()
    {
        return myDispatcher;
    }
    
    private boolean handleResponse(MessageContext messageContext) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = null;

            if (messageContext != null && messageContext.getMessage() != null)
            {
                Object aMessage = messageContext.getMessage();
                
                if (aMessage instanceof InputStream)
                {
                    aProtocolMessage = myProtocolFormatter.decodeMessage((InputStream)aMessage);
                }
                else
                {
                    aProtocolMessage = myProtocolFormatter.decodeMessage(aMessage);
                }
            }
            else
            {
                EneterTrace.warning(TracedObject() + "detected null response message. It means the listening to responses stopped.");
            }

            if (aProtocolMessage == null || aProtocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                EneterTrace.debug("CLIENT DISCONNECTED RECEIVED");
                myDispatchingAfterResponseReading.invoke(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        cleanAfterConnection(false);
                    }
                });
                
                return false;
            }
            else if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
            {
                EneterTrace.debug("RESPONSE MESSAGE RECEIVED");
                
                final Object aMessageData = aProtocolMessage.Message;
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
                                notifyResponseMessageReceived(aMessageData);
                            }
                        });
                    }
                });
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
            }

            return true;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void cleanAfterConnection(boolean sendCloseMessageFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            boolean aNotifyFlag = false;
            
            // If close connection is already being performed then do not close again.
            // E.g. CloseConnection() and this will stop a response listening thread. This thread would come here and
            //      would stop on the following lock -> deadlock.
            if (!myCloseConnectionIsRunning)
            {
                synchronized (myConnectionManipulatorLock)
                {
                    myCloseConnectionIsRunning = true;
                    
                    // Try to notify that the connection is closed
                    if (sendCloseMessageFlag)
                    {
                        try
                        {
                            // Send the close connection request.
                            SenderUtil.sendCloseConnection(myOutputConnector, getResponseReceiverId(), myProtocolFormatter);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                        }
                    }
    
                    try
                    {
                        myOutputConnector.closeConnection();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                    }
    
    
                     // Note: the notification must run outside the lock because of potententional deadlock.
                    aNotifyFlag = myConnectionIsCorrectlyOpen;
                    myConnectionIsCorrectlyOpen = false;
                    myCloseConnectionIsRunning = false;
                }
            }
        
            // Notify the connection closed only if it was successfuly open before.
            // E.g. It will be not notified if the CloseConnection() is called for already closed connection.
            if (aNotifyFlag)
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
    private boolean myStartReceiverAfterSendOpenRequest;
    private boolean myConnectionIsCorrectlyOpen;
    private boolean myCloseConnectionIsRunning;
    private Object myConnectionManipulatorLock = new Object();
    
    private IProtocolFormatter<?> myProtocolFormatter;

    private String myChannelId;
    private String myResponseReceiverId;
    private IThreadDispatcher myDispatcher;
    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceived = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOpened = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosed = new EventImpl<DuplexChannelEventArgs>();
    
    
    IFunction1<Boolean, MessageContext> myResponseHandler = new IFunction1<Boolean, MessageContext>()
    {
        @Override
        public Boolean invoke(MessageContext x) throws Exception
        {
            return handleResponse(x);
        }
    };
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " '" + myChannelId + "' ";
    }
}

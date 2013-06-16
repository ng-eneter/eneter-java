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

import eneter.messaging.dataprocessing.messagequeueing.internal.IInvoker;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.*;
import eneter.net.system.threading.internal.ThreadPool;


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
            IInvoker workingThreadInvoker,
            IProtocolFormatter<?> protocolFormatter,
            IClientConnectorFactory clientConnectorFactory,
            boolean startReceiverAfterSendOpenRequest)
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

                myWorkingThreadInvoker = workingThreadInvoker;
                myClientConnectorFactory = clientConnectorFactory;
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
                    myWorkingThreadInvoker.start();

                    // Create sender responsible for sending messages.
                    myClientConnector = myClientConnectorFactory.createClientConnector(getChannelId(), getResponseReceiverId());

                    if (!myStartReceiverAfterSendOpenRequest)
                    {
                        // Connect and start listening to response messages.
                        myClientConnector.openConnection(myResponseHandler);
                    }

                    // Send the open connection request.
                    SenderUtil.SendOpenConnection(myClientConnector, getResponseReceiverId(), myProtocolFormatter);

                    if (myStartReceiverAfterSendOpenRequest)
                    {
                        // Connect and start listening to response messages.
                        myClientConnector.openConnection(myResponseHandler);
                    }

                    // Invoke the event notifying, the connection was opened.
                    myConnectionIsCorrectlyOpen = true;
                    notify(myConnectionOpened);
                }
                catch (Exception err)
                {
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
            clearConnection(true);
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
            return (myClientConnector != null && myClientConnector.isConnected());
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
                    SenderUtil.sendMessage(myClientConnector, getResponseReceiverId(), message, myProtocolFormatter);
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
    
    private boolean handleResponse(MessageContext messageContext) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = null;

            Object aMessage = messageContext.getMessage();
            if (messageContext != null && aMessage != null)
            {
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

            // Invoke processing of the protocol message according to the thread model.
            final ProtocolMessage aProtocolMessageTmp = aProtocolMessage;
            myWorkingThreadInvoker.invoke(new IMethod()
            {
                @Override
                public void invoke() throws Exception
                {
                    handleResponseMessage(aProtocolMessageTmp);
                }
            });

            return aProtocolMessage != null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void handleResponseMessage(ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (protocolMessage == null || protocolMessage.MessageType == EProtocolMessageType.CloseConnectionRequest)
            {
                // Note: This is the working thread handling response messages.
                //       The following CloseConnection() will try to stop this thread. Therefore to avoid the deadlock
                //       CloseConnection() must be executed from a different thread.
                ThreadPool.queueUserWorkItem(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        clearConnection(false);
                    }
                });
            }
            else if (protocolMessage.MessageType == EProtocolMessageType.MessageReceived)
            {
                if (myResponseMessageReceived.isSubscribed())
                {
                    try
                    {
                        myResponseMessageReceived.raise(this, new DuplexChannelMessageEventArgs(getChannelId(), protocolMessage.Message, getResponseReceiverId(), ""));
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
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void clearConnection(boolean sendCloseMessageFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                // Try to notify that the connection is closed
                if (myClientConnector != null)
                {
                    if (sendCloseMessageFlag)
                    {
                        try
                        {
                            // Send the close connection request.
                            SenderUtil.sendCloseConnection(myClientConnector, getResponseReceiverId(), myProtocolFormatter);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                        }
                    }

                    try
                    {
                        myClientConnector.closeConnection();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.CloseConnectionFailure, err);
                    }

                    myClientConnector = null;
                }

                myWorkingThreadInvoker.stop();

                // Notify the connection closed only if it was successfuly open before.
                // E.g. It will be not notified if the CloseConnection() is called for already closed connection.
                if (myConnectionIsCorrectlyOpen)
                {
                    myConnectionIsCorrectlyOpen = false;
                    notify(myConnectionClosed);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notify(final EventImpl<DuplexChannelEventArgs> eventHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Runnable anInvoker = new Runnable()
            {
                @Override
                public void run()
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
            };

            // Invoke the event in a different thread.
            ThreadPool.queueUserWorkItem(anInvoker);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IClientConnectorFactory myClientConnectorFactory;
    private IClientConnector myClientConnector;
    private boolean myStartReceiverAfterSendOpenRequest;
    private boolean myConnectionIsCorrectlyOpen;
    private Object myConnectionManipulatorLock = new Object();
    
    private String myChannelId;
    private String myResponseReceiverId;
    
    private IProtocolFormatter<?> myProtocolFormatter;
    private IInvoker myWorkingThreadInvoker;
    
    
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

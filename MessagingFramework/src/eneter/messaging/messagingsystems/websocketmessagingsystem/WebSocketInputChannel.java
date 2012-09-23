/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.util.ArrayList;

import eneter.messaging.dataprocessing.messagequeueing.internal.IInvoker;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.net.system.*;


class WebSocketInputChannel extends WebSocketInputChannelBase implements IInputChannel
{
    @Override
    public Event<ChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEvent.getApi();
    }
 
    
    public WebSocketInputChannel(String ipAddressAndPort,
            IInvoker invoker,
            IServerSecurityFactory securityStreamFactory, IProtocolFormatter<?> protocolFormatter)
            throws Exception
    {
        super(ipAddressAndPort, invoker, securityStreamFactory);
        
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected void disconnectClients()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedSenders)
            {
                for (IWebSocketClientContext aConnection : myConnectedSenders)
                {
                    aConnection.closeConnection();
                }
                myConnectedSenders.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected void handleConnection(IWebSocketClientContext client)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectedSenders)
            {
                myConnectedSenders.add(client);
            }

            try
            {
                // Wait until a message starts to come.
                WebSocketMessage aWebSocketMessage = client.receiveMessage();

                if (aWebSocketMessage != null)
                {
                    // Read the message from the stream.
                    final ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(aWebSocketMessage.getInputStream());

                    if (aProtocolMessage != null)
                    {
                        if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                        {
                            // Notify message received from the working thread.
                            myMessageProcessingWorker.invoke(new IMethod()
                            {
                                @Override
                                public void invoke() throws Exception
                                {
                                    notifyMessageReceived(aProtocolMessage.Message);
                                }
                            });
                        }
                        else
                        {
                            EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                        }
                    }
                }
            }
            finally
            {
                synchronized (myConnectedSenders)
                {
                    myConnectedSenders.remove(client);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyMessageReceived(Object message)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myMessageReceivedEvent.isSubscribed())
            {
                try
                {
                    myMessageReceivedEvent.raise(this, new ChannelMessageEventArgs(myChannelId, message));
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
    
    
    private ArrayList<IWebSocketClientContext> myConnectedSenders = new ArrayList<IWebSocketClientContext>();
    
    private IProtocolFormatter<?> myProtocolFormatter;
    
    
    private EventImpl<ChannelMessageEventArgs> myMessageReceivedEvent = new EventImpl<ChannelMessageEventArgs>();
    
    
    @Override
    protected String TracedObject()
    {
        return "WebSocket input channel '" + getChannelId() + "' "; 
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.util.ArrayList;

import eneter.messaging.diagnostic.*;
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
 
    
    public WebSocketInputChannel(String ipAddressAndPort, IServerSecurityFactory securityStreamFactory, IProtocolFormatter<?> protocolFormatter)
            throws Exception
    {
        super(ipAddressAndPort, securityStreamFactory);
        
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
                    ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(aWebSocketMessage.getInputStream());

                    if (aProtocolMessage != null)
                    {
                        // Put the message to the queue from where the working thread removes it to notify
                        // subscribers of the input channel.
                        // Note: therfore subscribers of the input channel are notified allways in one thread.
                        myMessageProcessingThread.enqueueMessage(aProtocolMessage);
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
    
    @Override
    protected void handleMessage(ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (protocolMessage.MessageType != EProtocolMessageType.MessageReceived)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                return;
            }

            if (myMessageReceivedEvent.isSubscribed())
            {
                try
                {
                    myMessageReceivedEvent.raise(this, new ChannelMessageEventArgs(getChannelId(), protocolMessage.Message));
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

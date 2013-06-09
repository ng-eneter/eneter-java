/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.httpmessagingsystem;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;

import eneter.net.system.*;


class HttpInputChannel extends HttpInputChannelBase
                       implements IInputChannel
{
    
    public HttpInputChannel(String ipAddressAndPort, IProtocolFormatter<byte[]> protocolFormatter,
            IServerSecurityFactory serverSecurityFactory)
            throws Exception
    {
        super(ipAddressAndPort, serverSecurityFactory);
        
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
    public Event<ChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventImpl.getApi();
    }

    @Override
    protected void handleConnection(HttpRequestContext httpClientContext)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // First read the message to the buffer.
            byte[] aRequestMessage = httpClientContext.getRequestMessage();
                
            // Decode the incoming message.
            ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(aRequestMessage);
            if (aProtocolMessage != null)
            {
                if (aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                {
                    // Put the message to the queue from where the working thread removes it to notify
                    // subscribers of the input channel.
                    // Note: therefore subscribers of the input channel are notified always in one thread.
                    try
                    {
                        myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + "failed to enque the message.", err);
                    }
                }
                else
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
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
            
            if (myMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myMessageReceivedEventImpl.raise(this, new ChannelMessageEventArgs(getChannelId(), protocolMessage.Message, ""));
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
    
    
    
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    
    private EventImpl<ChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<ChannelMessageEventArgs>();
    

    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " '" + getChannelId() + "' "; 
    }

}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.messaging.dataprocessing.messagequeueing.internal.IInvoker;
import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.*;
import eneter.net.system.threading.internal.ThreadPool;

public class DefaultInputChannel extends DefaultInputChannelBase implements IInputChannel
{
    public Event<ChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventImpl.getApi();
    }

    
    public DefaultInputChannel(String channelId,
            IInvoker workingThreadInvoker,
            IProtocolFormatter<?> protocolFormatter,
            IServiceConnectorFactory serviceConnectorFactory) throws Exception
        {
            super(channelId, workingThreadInvoker, protocolFormatter, serviceConnectorFactory);
        }
    

    @Override
    protected void disconnectClients()
    {
        // n.a.
    }
    
    @Override
    protected boolean handleMessage(final MessageContext messageContext) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = null;

            if (messageContext != null)
            {
                aProtocolMessage = getProtocolMessage(messageContext.getMessage());
            }

            if (aProtocolMessage != null)
            {
                // Execute the processing of the message according to desired thread mode.
                final ProtocolMessage aProtocolMessageTmp = aProtocolMessage;
                myWorkingThreadInvoker.invoke(new IMethod()
                {
                    @Override
                    public void invoke() throws Exception
                    {
                        handleMessage(messageContext, aProtocolMessageTmp);
                    }
                });
            }

            return aProtocolMessage != null;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void handleMessage(MessageContext messageContext, ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (messageContext == null)
            {
                // Listening stopped.
                ThreadPool.queueUserWorkItem(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        stopListening();
                    }
                });
            }

            if (protocolMessage.MessageType != EProtocolMessageType.MessageReceived)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                return;
            }

            if (myMessageReceivedEventImpl != null)
            {
                try
                {
                    myMessageReceivedEventImpl.raise(this, new ChannelMessageEventArgs(myChannelId, protocolMessage.Message, ""));
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
    
    
    private EventImpl<ChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<ChannelMessageEventArgs>();
}

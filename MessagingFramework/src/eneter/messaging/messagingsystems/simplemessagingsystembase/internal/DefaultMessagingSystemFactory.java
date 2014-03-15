/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.*;

public class DefaultMessagingSystemFactory implements IMessagingSystemFactory
{
    public DefaultMessagingSystemFactory(IMessagingProvider messagingProvider, IProtocolFormatter<?> protocolFromatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myOutputConnectorFactory = new DefaultOutputConnectorFactory(messagingProvider);
            myInputConnectorFactory = new DefaultInputConnectorFactory(messagingProvider);
            myProtocolFormatter = protocolFromatter;

            NoDispatching aNoDispatching = new NoDispatching();
            myInputChannelThreading = aNoDispatching;
            myOutputChannelThreading = aNoDispatching;
            
            myDispatcherAfterMessageDecoded = aNoDispatching.getDispatcher();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, myDispatcherAfterMessageDecoded, myOutputConnectorFactory, myProtocolFormatter, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, myDispatcherAfterMessageDecoded, myOutputConnectorFactory, myProtocolFormatter, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myInputChannelThreading.getDispatcher();
            IInputConnector anInputConnector = myInputConnectorFactory.createInputConnector(channelId);
            return new DefaultDuplexInputChannel(channelId, aDispatcher, myDispatcherAfterMessageDecoded, anInputConnector, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public void setOutputChannelThreading(IThreadDispatcherProvider threadDispatcherProvider)
    {
        myOutputChannelThreading = threadDispatcherProvider;
    }
    
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return myOutputChannelThreading;
    }
    
    public void setInputChannelThreading(IThreadDispatcherProvider threadDispatcherProvider)
    {
        myInputChannelThreading = threadDispatcherProvider;
    }
    
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return myInputChannelThreading;
    }
    
    private IProtocolFormatter<?> myProtocolFormatter;
    private IOutputConnectorFactory myOutputConnectorFactory;
    private IInputConnectorFactory myInputConnectorFactory;
    private IThreadDispatcher myDispatcherAfterMessageDecoded;
    
    private IThreadDispatcherProvider myOutputChannelThreading;
    private IThreadDispatcherProvider myInputChannelThreading;
}

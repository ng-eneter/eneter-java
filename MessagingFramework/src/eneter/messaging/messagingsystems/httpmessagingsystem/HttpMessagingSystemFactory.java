/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.DefaultDuplexInputChannel;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.DefaultDuplexOutputChannel;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.IInputConnector;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.IInputConnectorFactory;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.IOutputConnector;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.IOutputConnectorFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.messaging.threading.dispatching.IThreadDispatcherProvider;
import eneter.messaging.threading.dispatching.SyncDispatching;

/**
 * Implements the messaging system delivering messages via HTTP.
 * 
 * It creates the communication channels using HTTP for sending and receiving messages.
 * The channel id must be a valid URI address. E.g.: http://127.0.0.1/something/ or https://127.0.0.1/something/. <br/>
 * Because HTTP is request-response based protocol, it does not keep the connection open.
 * Therefore, for the bidirectional communication used by duplex channels, the polling mechanism is used.
 * The duplex output channel regularly polls for response messages and the duplex input channel constantly measures the inactivity time
 * to recognize whether the duplex output channel is still connected.<br/><br/>
 * Notice, to start listening via input channel (or duplex input channel), the application must be executed with sufficient rights.
 * Otherwise the exception will be thrown.<br/>
 *
 */
public class HttpMessagingSystemFactory implements IMessagingSystemFactory
{
    private class HttpInputConnectorFactory implements IInputConnectorFactory
    {
        public HttpInputConnectorFactory(int responseReceiverInactivityTimeout, IServerSecurityFactory serverSecurityFactory)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myOutputConnectorInactivityTimeout = responseReceiverInactivityTimeout;
                myServerSecurityFactory = serverSecurityFactory;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IInputConnector createInputConnector(String inputConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new HttpInputConnector(inputConnectorAddress, myOutputConnectorInactivityTimeout, myServerSecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        private int myOutputConnectorInactivityTimeout;
        private IServerSecurityFactory myServerSecurityFactory;
    }
    
    private class HttpOutputConnectorFactory implements IOutputConnectorFactory
    {
        public HttpOutputConnectorFactory(int pollingFrequency)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myPollingFrequency = pollingFrequency;
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public IOutputConnector createOutputConnector(String inputConnectorAddress, String outputConnectorAddress)
                throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                return new HttpOutputConnector(inputConnectorAddress, outputConnectorAddress, myPollingFrequency);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        private int myPollingFrequency;
    }
    
    
    /**
     * Constructs the factory that will create channels with default settings.
     * 
     * The polling frequency will be 500 ms and the inactivity timeout will be 10 minutes.<br/>
     * <br/>
     * The polling frequency and the inactivity time are used only by duplex channels.
     * The polling frequency specifies how often the duplex output channel checks if there are pending response messages.
     * The inactivity time is measured by the duplex input channel and specifies the maximum time, the duplex output channel
     * does not have to poll for messages.
     * If the inactivity time is exceeded, considers the duplex output channel as disconnected.
     */
    public HttpMessagingSystemFactory()
    {
        this(500, 600000, new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the factory that will create channel with specified settings.
     * 
     * The polling frequency will be 500 ms and the inactivity timeout will be 10 minutes.<br/>
     * <br/>
     * The polling frequency and the inactivity time are used only by duplex channels.
     * The polling frequency specifies how often the duplex output channel checks if there are pending response messages.
     * The inactivity time is measured by the duplex input channel and specifies the maximum time, the duplex output channel
     * does not have to poll for messages.
     * If the inactivity time is exceeded, considers the duplex output channel as disconnected.
     * 
     * @param pollingFrequency how often the duplex output channel polls for the pending response messages
     * @param inactivityTimeout maximum time (measured by duplex input channel),
     *    the duplex output channel does not have to poll
     *    for response messages. If the time is exceeded, the duplex output channel is considered as disconnected.
     */
    public HttpMessagingSystemFactory(int pollingFrequency, int inactivityTimeout)
    {
        this(pollingFrequency, inactivityTimeout, new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the factory that will create channel with specified settings.
     * 
     * The polling frequency will be 500 ms and the inactivity timeout will be 10 minutes.<br/>
     * <br/>
     * The polling frequency and the inactivity time are used only by duplex channels.
     * The polling frequency specifies how often the duplex output channel checks if there are pending response messages.
     * The inactivity time is measured by the duplex input channel and specifies the maximum time, the duplex output channel
     * does not have to poll for messages.
     * If the inactivity time is exceeded, considers the duplex output channel as disconnected.<br/>
     * 
     * @param pollingFrequency how often the duplex output channel polls for the pending response messages
     * @param inactivityTimeout maximum time (measured by duplex input channel),
     *    the duplex output channel does not have to poll
     *    for response messages. If the time is exceeded, the duplex output channel is considered as disconnected.
     * @param protocolFormatter formatter for low-level messages between duplex output channel and duplex input channel
     */
    public HttpMessagingSystemFactory(int pollingFrequency, int inactivityTimeout, IProtocolFormatter<byte[]> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myPollingFrequency = pollingFrequency;
            myInactivityTimeout = inactivityTimeout;
            myProtocolFormatter = protocolFormatter;

            myInputChannelThreading = new SyncDispatching();
            myOutputChannelThreading = myInputChannelThreading;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using HTTP.
     * 
     * The channel id must be a valid URI address e.g. http://127.0.0.1:8090/something/<br/>
     * <br/>
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method generates the unique response receiver id automatically.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IOutputConnectorFactory aClientConnectorFactory = new HttpOutputConnectorFactory(myPollingFrequency);
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, aClientConnectorFactory, myProtocolFormatter, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using HTTP.
     * 
     * The channel id must be a valid URI address e.g. http://127.0.0.1:8090/something/<br/>
     * <br/>
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method allows to specified a desired response receiver id. Please notice, the response receiver
     * id is supposed to be unique.<br/>
     * <br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myOutputChannelThreading.getDispatcher();
            IOutputConnectorFactory aClientConnectorFactory = new HttpOutputConnectorFactory(myPollingFrequency);
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, aClientConnectorFactory, myProtocolFormatter, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending back response messages by using HTTP.
     * 
     * The channel id must be a valid URI address e.g. http://127.0.0.1:8090/something/<br/>
     * <br/>
     * The duplex input channel is intended for the bidirectional communication.
     * It can receive messages from the duplex output channel and send back response messages.
     * <br/><br/>
     * The duplex input channel can communicate only with the duplex output channel and not with the output channel.
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IThreadDispatcher aDispatcher = myInputChannelThreading.getDispatcher();
            IServerSecurityFactory aServerSecurityFactory = getServerSecurityFactory(channelId);
            IInputConnectorFactory anInputConnectorFactory = new HttpInputConnectorFactory(myInactivityTimeout, aServerSecurityFactory);
            IInputConnector anInputConnector = anInputConnectorFactory.createInputConnector(channelId);
            return new DefaultDuplexInputChannel(channelId, aDispatcher, anInputConnector, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public IProtocolFormatter<?> getProtocolFormatter()
    {
        return myProtocolFormatter;
    }
    
    private IServerSecurityFactory getServerSecurityFactory(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aProtocol = new URL(channelId).getProtocol().toLowerCase();
            if (aProtocol.equals("https"))
            {
                return  new SslServerFactory();
            }
            else
            {
                return new NoneSecurityServerFactory();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        } 
    }

    
    
    public HttpMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myInputChannelThreading = inputChannelThreading;
            return this;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myInputChannelThreading;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public HttpMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myOutputChannelThreading = outputChannelThreading;
            return this;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myOutputChannelThreading;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
        
    
    private int myPollingFrequency;
    private int myInactivityTimeout;
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.messaging.threading.dispatching.*;
import eneter.messaging.threading.dispatching.SyncDispatching;

/**
 * Messaging system delivering messages via HTTP.
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
        public HttpInputConnectorFactory(IProtocolFormatter protocolFormatter, int responseReceiverInactivityTimeout, IServerSecurityFactory serverSecurityFactory)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myProtocolFormatter = protocolFormatter;
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
                return new HttpInputConnector(inputConnectorAddress, myProtocolFormatter, myOutputConnectorInactivityTimeout, myServerSecurityFactory);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        private IProtocolFormatter myProtocolFormatter;
        private int myOutputConnectorInactivityTimeout;
        private IServerSecurityFactory myServerSecurityFactory;
    }
    
    private class HttpOutputConnectorFactory implements IOutputConnectorFactory
    {
        public HttpOutputConnectorFactory(IProtocolFormatter protocolFormatter, int pollingFrequency)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myProtocolFormatter = protocolFormatter;
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
                return new HttpOutputConnector(inputConnectorAddress, outputConnectorAddress, myProtocolFormatter, myPollingFrequency);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        private IProtocolFormatter myProtocolFormatter;
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
    public HttpMessagingSystemFactory(int pollingFrequency, int inactivityTimeout, IProtocolFormatter protocolFormatter)
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
     * Creates duplex output channel which can send and receive messages from the duplex input channel using HTTP.
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
            IThreadDispatcher aDispatcherAfterMessageDecoded = myDispatchingAfterMessageDecoded.getDispatcher();
            IOutputConnectorFactory aClientConnectorFactory = new HttpOutputConnectorFactory(myProtocolFormatter, myPollingFrequency);
            return new DefaultDuplexOutputChannel(channelId, null, aDispatcher, aDispatcherAfterMessageDecoded, aClientConnectorFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates duplex output channel which can send and receive messages from the duplex input channel using HTTP.
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
            IThreadDispatcher aDispatcherAfterMessageDecoded = myDispatchingAfterMessageDecoded.getDispatcher();
            IOutputConnectorFactory aClientConnectorFactory = new HttpOutputConnectorFactory(myProtocolFormatter, myPollingFrequency);
            return new DefaultDuplexOutputChannel(channelId, responseReceiverId, aDispatcher, aDispatcherAfterMessageDecoded, aClientConnectorFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex input channel which can receive and send messages to the duplex output channel using UDP.
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
            IInputConnectorFactory anInputConnectorFactory = new HttpInputConnectorFactory(myProtocolFormatter, myInactivityTimeout, aServerSecurityFactory);
            IInputConnector anInputConnector = anInputConnectorFactory.createInputConnector(channelId);
            IThreadDispatcher aDispatcherAfterMessageDecoded = myDispatchingAfterMessageDecoded.getDispatcher();
            return new DefaultDuplexInputChannel(channelId, aDispatcher, aDispatcherAfterMessageDecoded, anInputConnector);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
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

    
    /**
     * Sets threading mode for input channels.
     * @param inputChannelThreading threading model
     * @return
     */
    public HttpMessagingSystemFactory setInputChannelThreading(IThreadDispatcherProvider inputChannelThreading)
    {
        myInputChannelThreading = inputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for input channels.
     * @return
     */
    public IThreadDispatcherProvider getInputChannelThreading()
    {
        return myInputChannelThreading;
    }
    
    /**
     * Sets threading mode for output channels.
     * @param outputChannelThreading
     * @return
     */
    public HttpMessagingSystemFactory setOutputChannelThreading(IThreadDispatcherProvider outputChannelThreading)
    {
        myOutputChannelThreading = outputChannelThreading;
        return this;
    }
    
    /**
     * Gets threading mode used for output channels.
     * @return
     */
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return myOutputChannelThreading;
    }
        
    
    private int myPollingFrequency;
    private int myInactivityTimeout;
    private IProtocolFormatter myProtocolFormatter;
    private IThreadDispatcherProvider myInputChannelThreading;
    private IThreadDispatcherProvider myOutputChannelThreading;
    private IThreadDispatcherProvider myDispatchingAfterMessageDecoded = new SyncDispatching();
}

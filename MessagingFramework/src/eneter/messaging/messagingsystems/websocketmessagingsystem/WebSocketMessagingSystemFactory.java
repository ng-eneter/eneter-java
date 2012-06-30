/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;


/**
 * Implements the messaging system delivering messages via websockets.
 * It creates the communication channels using WebSockets for sending and receiving messages.
 * The channel id must be a valid URI address. E.g.: ws://127.0.0.1:6080/MyService/. <br/>
 *
 */
public class WebSocketMessagingSystemFactory implements IMessagingSystemFactory
{
    /**
     * Constructs the websocket messaging factory.
     */
    public WebSocketMessagingSystemFactory()
    {
        this(new EneterProtocolFormatter());
    }

    /**
     * Constructs the websocket messaging factory.
     * 
     * @param protocolFormatter formatter used for low-level messages between duplex output and duplex input channels.
     */
    public WebSocketMessagingSystemFactory(IProtocolFormatter<?> protocolFormatter)
    {
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
    
    /**
     * Creates the output channel sending messages to the specified input channel by using WebSocket.
     * The output channel can send messages only to the input channel and not to the duplex input channel.
     * 
     * @param channelId Identifies the receiving output channel. The channel id must be a valid URI address e.g. ws://127.0.0.1:8090/MyService/
     */
    @Override
    public IOutputChannel createOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new WebSocketOutputChannel(channelId, myClientSecurityFactory, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the input channel receiving messages from output channel by using WebSocket.
     * @param channelId The addres, the input channel will listen to. The channel id must be a valid URI address e.g. ws://127.0.0.1:8090/MyService/.
     */
    @Override
    public IInputChannel createInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new WebSocketInputChannel(channelId, myServerSecurityFactory, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using WebSocket.
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method generates the unique response receiver id automatically.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     * 
     * @param channelId Identifies the receiving duplex input channel. The channel id must be a valid URI address e.g. ws://127.0.0.1:8090/MyService/ 
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new WebSocketDuplexOutputChannel(channelId, null, myClientSecurityFactory, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the duplex output channel sending messages to the duplex input channel and receiving response messages by using WebSocket.
     * The duplex output channel is intended for the bidirectional communication.
     * Therefore, it can send messages to the duplex input channel and receive response messages.
     * <br/><br/>
     * The duplex input channel distinguishes duplex output channels according to the response receiver id.
     * This method allows to specified a desired response receiver id. Please notice, the response receiver
     * id is supposed to be unique.
     * <br/><br/>
     * The duplex output channel can communicate only with the duplex input channel and not with the input channel.
     * 
     * @param channelId Identifies the receiving duplex input channel. The channel id must be a valid URI address e.g. ws://127.0.0.1:8090/MyService/ 
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new WebSocketDuplexOutputChannel(channelId, responseReceiverId, myClientSecurityFactory, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Creates the duplex input channel receiving messages from the duplex output channel and sending back response messages by using WebSocket.
     * The duplex input channel is intended for the bidirectional communication.
     * It can receive messages from the duplex output channel and send back response messages.
     * <br/><br/>
     * The duplex input channel can communicate only with the duplex output channel and not with the output channel.
     * 
     * @param channelId Identifies this duplex input channel. The channel id must be a valid URI address (e.g. ws://127.0.0.1:8090/MyService/) the input channel will listen to.
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new WebSocketDuplexInputChannel(channelId, myServerSecurityFactory, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    /**
     * Sets the factory that will be used for creation of secured server socket.
     * @param serverSecurityFactory
     */
    public void setServerSecurity(IServerSecurityFactory serverSecurityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myServerSecurityFactory = serverSecurityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Sets the factory that will be used for creation of secured client socket.
     * @param clientSecurityFactory
     */
    public void setClientSecurity(IClientSecurityFactory clientSecurityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myClientSecurityFactory = clientSecurityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private IServerSecurityFactory myServerSecurityFactory = new NoneSecurityServerFactory();
    private IClientSecurityFactory myClientSecurityFactory = new NoneSecurityClientFactory();

    private IProtocolFormatter<?> myProtocolFormatter;
}

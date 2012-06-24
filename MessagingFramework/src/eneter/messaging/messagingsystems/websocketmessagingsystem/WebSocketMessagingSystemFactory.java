package eneter.messaging.messagingsystems.websocketmessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;

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

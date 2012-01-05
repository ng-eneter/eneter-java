package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;

public class HttpMessagingSystemFactory implements IMessagingSystemFactory
{
    public HttpMessagingSystemFactory()
    {
        this(500, 600000, new EneterProtocolFormatter());
    }
    
    public HttpMessagingSystemFactory(int pollingFrequency, int inactivityTimeout)
    {
        this(pollingFrequency, inactivityTimeout, new EneterProtocolFormatter());
    }
    
    public HttpMessagingSystemFactory(int pollingFrequency, int inactivityTimeout, IProtocolFormatter<byte[]> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myPollingFrequency = pollingFrequency;
            myConnectedDuplexOutputChannelInactivityTimeout = inactivityTimeout;
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public IOutputChannel createOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new HttpOutputChannel(channelId, myProtocolFormatter);
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
            IServerSecurityFactory aServerSecurityFactory = getServerSecurityFactory(channelId);
            return new HttpInputChannel(channelId, myProtocolFormatter, aServerSecurityFactory);
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
            return new HttpDuplexOutputChannel(channelId, null, myPollingFrequency, myProtocolFormatter);
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
            return new HttpDuplexOutputChannel(channelId, responseReceiverId, myPollingFrequency, myProtocolFormatter);
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
            IServerSecurityFactory aServerSecurityFactory = getServerSecurityFactory(channelId);
            return new HttpDuplexInputChannel(channelId, myConnectedDuplexOutputChannelInactivityTimeout, myProtocolFormatter, aServerSecurityFactory);
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

    private IProtocolFormatter<byte[]> myProtocolFormatter;
    private int myConnectedDuplexOutputChannelInactivityTimeout;
    private int myPollingFrequency;
}

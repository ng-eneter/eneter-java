package eneter.messaging.messagingsystems.androidusbcablemessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.EneterProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;

public class AndroidUsbCableMessagingFactory implements IMessagingSystemFactory
{
    public AndroidUsbCableMessagingFactory()
    {
        this(5037, new EneterProtocolFormatter());
    }
    
    public AndroidUsbCableMessagingFactory(int adbHostPort, IProtocolFormatter<byte[]> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAdbHostPort = adbHostPort;
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
        throw new UnsupportedOperationException("One-way output channel is not supported for Android USB cable messaging.");
    }

    @Override
    public IInputChannel createInputChannel(String channelId) throws Exception
    {
        throw new UnsupportedOperationException("One-way input channel is not supported for Android USB cable messaging.");
    }

    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new AndroidUsbDuplexOutputChannel(Integer.parseInt(channelId), null, myAdbHostPort, myProtocolFormatter);
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
            return new AndroidUsbDuplexOutputChannel(Integer.parseInt(channelId), responseReceiverId, myAdbHostPort, myProtocolFormatter);
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
        throw new UnsupportedOperationException("Duplex input channel is not supported for Android USB cable messaging.");
    }

    
    private int myAdbHostPort;
    private IProtocolFormatter<byte[]> myProtocolFormatter;
}

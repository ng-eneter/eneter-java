package eneter.messaging.messagingsystems.androidusbcablemessagingsystem;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.UUID;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;

class AndroidUsbDuplexOutputChannel implements IDuplexOutputChannel
{
    @Override
    public Event<DuplexChannelMessageEventArgs> responseMessageReceived()
    {
        return myResponseMessageReceivedEventImpl.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        return myConnectionOpenedEventImpl.getApi();
    }

    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosedEventImpl.getApi();
    }
    
    
    public AndroidUsbDuplexOutputChannel(int port, String responseReceiverId, int adbHostPort, IProtocolFormatter<byte[]> protocolFormatter) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myChannelId = Integer.toString(port);
            myResponseReceiverId = (StringExt.isNullOrEmpty(responseReceiverId)) ? port + "_" + UUID.randomUUID().toString() : responseReceiverId;
            myAdbHostPort = adbHostPort;

            IMessagingSystemFactory aTcpMessaging = new TcpMessagingSystemFactory();
            String anIpAddressAndPort = "tcp://127.0.0.1:" + myChannelId + "/";
            myOutputchannel = aTcpMessaging.createDuplexOutputChannel(anIpAddressAndPort, myResponseReceiverId);
            myOutputchannel.connectionOpened().subscribe(myOnConnectionOpenedHandler);
            myOutputchannel.connectionClosed().subscribe(myOnConnectionClosedHandler);
            myOutputchannel.responseMessageReceived().subscribe(myOnResponseMessageReceivedHandler);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public String getChannelId()
    {
        return myChannelId;
    }

    @Override
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }
    
    @Override
    public IThreadDispatcher getDispatcher()
    {
        return myOutputchannel.getDispatcher();
    }

    @Override
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myOutputchannel.sendMessage(message);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void openConnection() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Configure the ADB host to forward communication via the USB cable.
            configureAdbToForwardCommunication(getChannelId());

            // Open connection with the service running on the Android device.
            myOutputchannel.openConnection();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void closeConnection()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Close connection with the service running on the Android device.
            myOutputchannel.closeConnection();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isConnected()
    {
        return myOutputchannel.isConnected();
    }
    
    private void configureAdbToForwardCommunication(String portNumber) throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Charset anAsciiCharset = Charset.forName("US-ASCII");
            
            String aForwardRequest = String.format("host:forward:tcp:%s;tcp:%s", portNumber, portNumber);


            // Open TCP connection with ADB host.
            InetSocketAddress anAdbHostAddress = new InetSocketAddress("127.0.0.1", myAdbHostPort);
            Socket aTcpClient = new Socket();
            try
            {
                aTcpClient.connect(anAdbHostAddress, 5000);
    
                // Encode the message for the ADB host.
                String anAdbRequest = String.format("%04X%s\n", aForwardRequest.length(), aForwardRequest);
                byte[] aRequestContent = anAdbRequest.getBytes(anAsciiCharset);
    
                aTcpClient.getOutputStream().write(aRequestContent);
    
                // Read the response from the ADB host.
                DataInputStream aReader = new DataInputStream(aTcpClient.getInputStream());
                byte[] aResponseContent = new byte[4];
                aReader.readFully(aResponseContent, 0, aResponseContent.length);
                String aResponse = new String(aResponseContent, anAsciiCharset); 
    
                // If ADB response indicates something was wrong.
                if (!aResponse.toUpperCase().equals("OKAY"))
                {
                    // Try to get the reason why it failed.
                    byte[] aLengthBuf = new byte[4]; 
                    aReader.readFully(aLengthBuf, 0, aLengthBuf.length);
                    String aLengthStr = new String(aLengthBuf, anAsciiCharset);
    
                    int aReasonMessageLength = 0;
                    try
                    {
                        aReasonMessageLength = Integer.parseInt(aLengthStr, 16);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to parse '" + aLengthStr + "' into a number. A hex format of number was expected.");
                    }
    
                    String aReasonStr = "";
                    if (aReasonMessageLength > 0)
                    {
                        // Read the content of the error reason message.
                        byte[] aReasonBuf = new byte[aReasonMessageLength];
                        aReader.readFully(aReasonBuf, 0, aReasonMessageLength);
                        aReasonStr = new String(aReasonBuf, anAsciiCharset);
                    }
    
                    String anErrorMessage = TracedObject() + "failed to configure the ADB host for forwarding the communication. The ADB responded: " + aResponse + " " + aReasonStr;
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }
            }
            finally
            {
                aTcpClient.close();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void onConnectionOpened(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notify(myConnectionOpenedEventImpl);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onConnectionClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            notify(myConnectionClosedEventImpl);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    DuplexChannelMessageEventArgs aMsg = new DuplexChannelMessageEventArgs(getChannelId(), e.getMessage(), getResponseReceiverId(), ""); 
                    myResponseMessageReceivedEventImpl.raise(this, aMsg);
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
    
    private void notify(final EventImpl<DuplexChannelEventArgs> eventHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                if (eventHandler.isSubscribed())
                {
                    DuplexChannelEventArgs aMsg = new DuplexChannelEventArgs(getChannelId(), getResponseReceiverId(), "");
                    eventHandler.raise(this, aMsg);
                }
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private IDuplexOutputChannel myOutputchannel;
    private int myAdbHostPort;
    private String myChannelId;
    private String myResponseReceiverId;
    
    private EventImpl<DuplexChannelMessageEventArgs> myResponseMessageReceivedEventImpl = new EventImpl<DuplexChannelMessageEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEventImpl = new EventImpl<DuplexChannelEventArgs>();
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionOpenedHandler = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionOpened(sender, e);
        }
    };
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionClosedHandler = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionClosed(sender, e);
        }
    };
            
    private EventHandler<DuplexChannelMessageEventArgs> myOnResponseMessageReceivedHandler = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onResponseMessageReceived(sender, e);
        }
    };
    
    private String TracedObject()
    {
        String aChannelId = (getChannelId() != null) ? getChannelId() : "";
        return getClass().getSimpleName() + " '" + aChannelId + "' ";
    }

    
}

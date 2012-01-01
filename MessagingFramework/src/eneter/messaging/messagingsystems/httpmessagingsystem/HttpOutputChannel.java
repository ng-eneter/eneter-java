package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.OutputStream;
import java.net.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.StringExt;

public class HttpOutputChannel implements IOutputChannel
{
    public HttpOutputChannel(String channelId, IProtocolFormatter<byte[]> protocolFormatter)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(channelId))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }

            try
            {
                myUrl = new URL(channelId);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            myChannelId = channelId;
            myProtocolFormatter = protocolFormatter;
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
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myLock)
            {
                try
                {
                    HttpURLConnection aConnection = (HttpURLConnection)myUrl.openConnection();
                    try
                    {
                        aConnection.setDoOutput(true);
                        aConnection.setRequestMethod("POST");

                        // Encode the message.
                        byte[] anEncodedMessage = myProtocolFormatter.encodeMessage("", message);
                        
                        // Write the message to the stream.
                        OutputStream aSender = aConnection.getOutputStream();
                        aSender.write(anEncodedMessage);
                        
                        // Fire the message.
                        // Note: requesting the response code will fire the message.
                        int aResponseCode = aConnection.getResponseCode();
                        if (aResponseCode != 200)
                        {
                            String aResponseMessage = aConnection.getResponseMessage();
                            throw new IllegalStateException(aResponseMessage);
                        }
                       
                    }
                    finally
                    {
                        if (aConnection != null)
                        {
                            aConnection.disconnect();
                        }
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                    throw err;
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    
    private URL myUrl;
    private Object myLock = new Object();
    
    private String myChannelId;
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    
    
    private String TracedObject()
    {
        return "The Http output channel '" + getChannelId() + "' "; 
    }
}

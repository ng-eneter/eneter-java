/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.URI;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IClientSecurityFactory;
import eneter.net.system.StringExt;

class WebSocketOutputChannel implements IOutputChannel
{
    
    public WebSocketOutputChannel(String ipAddressAndPort, IClientSecurityFactory serverSecurityFactory, IProtocolFormatter<?> protocolFormatter)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(ipAddressAndPort))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }

            try
            {
                // just check if the address is valid
                myUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            mySecurityStreamFactory = serverSecurityFactory;

            myChannelId = ipAddressAndPort;

            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        } 
    }

    public String getChannelId()
    {
        return myChannelId;
    }
    
    public void sendMessage(Object message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (this)
            {
                WebSocketClient aWebScoketClient = new WebSocketClient(myUri, mySecurityStreamFactory);

                try
                {
                    aWebScoketClient.openConnection();

                    // Encode the message.
                    Object anEncodedMessage = myProtocolFormatter.encodeMessage("", message);
                    
                    // Send the message.
                    aWebScoketClient.sendMessage(anEncodedMessage);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                    throw err;
                }
                finally
                {
                    aWebScoketClient.closeConnection();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private String myChannelId;
    private URI myUri;
    private IClientSecurityFactory mySecurityStreamFactory;
    private IProtocolFormatter<?> myProtocolFormatter;
    
    private String TracedObject()
    {
        return "WebSocket output channel '" + getChannelId() + "' ";
    }
}

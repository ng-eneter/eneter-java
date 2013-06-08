/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.security.InvalidParameterException;

import eneter.messaging.diagnostic.*;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;
import eneter.net.system.internal.StringExt;

public class DefaultOutputChannel implements IOutputChannel
{
    public DefaultOutputChannel(String channelId, IProtocolFormatter<?> protocolFormatter, IClientConnectorFactory clientConnectorFactory)
    {
        if (StringExt.isNullOrEmpty(channelId))
        {
            EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
            throw new InvalidParameterException(ErrorHandler.NullOrEmptyChannelId);
        }
        
        myChannelId = channelId;
        myProtocolFormatter = protocolFormatter;
        myClientConnectorFactory = clientConnectorFactory;
    }
    
    public String getChannelId()
    {
        return myChannelId;
    }

    public void sendMessage(Object message)
        throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            IClientConnector aClientConnector = null;
            try
            {
                // Send the message.
                aClientConnector = myClientConnectorFactory.createClientConnector(getChannelId(), null);
                aClientConnector.openConnection(null);

                SenderUtil.sendMessage(aClientConnector, "", message, myProtocolFormatter);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                throw err;
            }
            finally
            {
                if (aClientConnector != null)
                {
                    aClientConnector.closeConnection();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private IClientConnectorFactory myClientConnectorFactory;
    private String myChannelId;
    private IProtocolFormatter<?> myProtocolFormatter;
   
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " '" + myChannelId + "' ";
    }
}

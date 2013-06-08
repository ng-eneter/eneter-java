/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.io.OutputStream;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.net.system.IMethod1;

public class SenderUtil
{
    public static void SendOpenConnection(ISender sender, final String responseReceiverId, final IProtocolFormatter<?> protocolFormatter)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (sender.isStreamWritter())
            {
                sender.sendMessage(new IMethod1<OutputStream>()
                {
                    @Override
                    public void invoke(OutputStream x) throws Exception
                    {
                        protocolFormatter.encodeOpenConnectionMessage(responseReceiverId, x);
                    }
                });
            }
            else
            {
                Object anEncodedMessage = protocolFormatter.encodeOpenConnectionMessage(responseReceiverId);
                sender.sendMessage(anEncodedMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static void sendCloseConnection(ISender sender, final String responseReceiverId, final IProtocolFormatter<?> protocolFormatter)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (sender.isStreamWritter())
            {
                sender.sendMessage(new IMethod1<OutputStream>()
                {
                    @Override
                    public void invoke(OutputStream x) throws Exception
                    {
                        protocolFormatter.encodeCloseConnectionMessage(responseReceiverId, x);
                    }
                });
            }
            else
            {
                Object anEncodedMessage = protocolFormatter.encodeCloseConnectionMessage(responseReceiverId);
                sender.sendMessage(anEncodedMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static void sendMessage(ISender sender, final String responseReceiverId, final Object message, final IProtocolFormatter<?> protocolFormatter)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {

            if (sender.isStreamWritter())
            {
                sender.sendMessage(new IMethod1<OutputStream>()
                {
                    @Override
                    public void invoke(OutputStream x) throws Exception
                    {
                        protocolFormatter.encodeMessage(responseReceiverId, message, x);
                    }
                });
            }
            else
            {
                Object anEncodedMessage = protocolFormatter.encodeMessage(responseReceiverId, message);
                sender.sendMessage(anEncodedMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
}

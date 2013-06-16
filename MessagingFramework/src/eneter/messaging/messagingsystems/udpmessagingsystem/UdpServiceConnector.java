/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.udpmessagingsystem;

import java.net.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.simplemessagingsystembase.internal.*;
import eneter.net.system.IFunction1;
import eneter.net.system.internal.StringExt;

class UdpServiceConnector implements IServiceConnector
{
    public UdpServiceConnector(String ipAddressAndPort) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (StringExt.isNullOrEmpty(ipAddressAndPort))
            {
                EneterTrace.error(ErrorHandler.NullOrEmptyChannelId);
                throw new IllegalArgumentException(ErrorHandler.NullOrEmptyChannelId);
            }

            URI aUri;
            try
            {
                // just check if the address is valid
                aUri = new URI(ipAddressAndPort);
            }
            catch (Exception err)
            {
                EneterTrace.error(ipAddressAndPort + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }

            myServiceEndpoint = new InetSocketAddress(aUri.getHost(), aUri.getPort()); 
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void startListening(
            IFunction1<Boolean, MessageContext> messageHandler) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListenerManipulatorLock)
            {
                myReceiver = new UdpReceiver(myServiceEndpoint, true);
                myReceiver.startListening(messageHandler);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListenerManipulatorLock)
            {
                if (myReceiver != null)
                {
                    myReceiver.stopListening();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening()
    {
        synchronized (myListenerManipulatorLock)
        {
            return myReceiver != null && myReceiver.isListening();
        }
    }

    @Override
    public ISender createResponseSender(String responseReceiverAddress)
    {
        throw new UnsupportedOperationException("CreateResponseSender is not supported in UdpServiceConnector.");
    }

    
    private InetSocketAddress myServiceEndpoint;
    private UdpReceiver myReceiver;
    private Object myListenerManipulatorLock = new Object();
}

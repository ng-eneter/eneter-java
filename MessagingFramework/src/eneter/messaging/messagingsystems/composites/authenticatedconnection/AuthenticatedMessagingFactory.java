/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;

public class AuthenticatedMessagingFactory implements IMessagingSystemFactory
{
    public AuthenticatedMessagingFactory(IMessagingSystemFactory underlyingMessagingSystem,
            IGetLoginMessage getLoginMessageCallback,
            IGetHandshakeResponseMessage getHandshakeResponseMessageCallback)
    {
        this(underlyingMessagingSystem, getLoginMessageCallback, getHandshakeResponseMessageCallback, null, null);
    }
    
    public AuthenticatedMessagingFactory(IMessagingSystemFactory underlyingMessagingSystem,
            IGetHanshakeMessage getHandshakeMessageCallback,
            IAuthenticate verifyHandshakeResponseMessageCallback)
    {
        this(underlyingMessagingSystem, null, null, getHandshakeMessageCallback, verifyHandshakeResponseMessageCallback);
    }
    
    public AuthenticatedMessagingFactory(IMessagingSystemFactory underlyingMessagingSystem,
            IGetLoginMessage getLoginMessageCallback,
            IGetHandshakeResponseMessage getHandshakeResponseMessageCallback,
            IGetHanshakeMessage getHandshakeMessageCallback,
            IAuthenticate verifyHandshakeResponseMessageCallback)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingMessaging = underlyingMessagingSystem;
            myAuthenticationTimeout = 10000;

            myGetLoginMessageCallback = getLoginMessageCallback;
            myGetHandShakeMessageCallback = getHandshakeMessageCallback;
            myGetHandshakeResponseMessageCallback = getHandshakeResponseMessageCallback;
            myAuthenticateCallback = verifyHandshakeResponseMessageCallback;
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
            if (myGetLoginMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex output channel because the callback to get the login message is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            if (myGetHandshakeResponseMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex output channel because the callback to get the response message for handshake is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            IDuplexOutputChannel anUnderlyingOutputChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId);
            return new AuthenticatedDuplexOutputChannel(anUnderlyingOutputChannel, myGetLoginMessageCallback, myGetHandshakeResponseMessageCallback, myAuthenticationTimeout);
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
            if (myGetLoginMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex output channel because the callback to get the login message is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            if (myGetHandshakeResponseMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex output channel because the callback to get the response message for handshake is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            IDuplexOutputChannel anUnderlyingOutputChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId, responseReceiverId);
            return new AuthenticatedDuplexOutputChannel(anUnderlyingOutputChannel, myGetLoginMessageCallback, myGetHandshakeResponseMessageCallback, myAuthenticationTimeout);
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
            if (myGetHandShakeMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex input channel because the callback to get the handshake message is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            if (myAuthenticateCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex input channel because the callback to verify the handshake response message is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            IDuplexInputChannel anUnderlyingInputChannel = myUnderlyingMessaging.createDuplexInputChannel(channelId);
            return new AuthenticatedDuplexInputChannel(anUnderlyingInputChannel, myGetHandShakeMessageCallback, myAuthenticateCallback);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    public AuthenticatedMessagingFactory setAuthenticationTimeout(long authenticationTimeout)
    {
        myAuthenticationTimeout = authenticationTimeout;
        return this;
    }
    
    
    private IMessagingSystemFactory myUnderlyingMessaging;

    private IGetLoginMessage myGetLoginMessageCallback;
    private IGetHanshakeMessage myGetHandShakeMessageCallback;
    private IGetHandshakeResponseMessage myGetHandshakeResponseMessageCallback;
    private IAuthenticate myAuthenticateCallback;
    
    private long myAuthenticationTimeout;
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName();
    }
}

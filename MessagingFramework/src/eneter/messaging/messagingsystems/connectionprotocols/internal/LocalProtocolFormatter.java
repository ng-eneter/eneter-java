/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.connectionprotocols.internal;

import java.io.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.*;


public class LocalProtocolFormatter implements IProtocolFormatter
{

    @Override
    public Object encodeOpenConnectionMessage(String responseReceiverId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.OpenConnectionRequest, responseReceiverId, null);
            return aProtocolMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void encodeOpenConnectionMessage(String responseReceiverId, OutputStream outputSream) throws Exception
    {
        throw new UnsupportedOperationException("This protocol formatter does not support encoding to stream.");
    }

    @Override
    public Object encodeCloseConnectionMessage(String responseReceiverId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.CloseConnectionRequest, responseReceiverId, null);
            return aProtocolMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void encodeCloseConnectionMessage(String responseReceiverId,
            OutputStream outputSream) throws Exception
    {
        throw new UnsupportedOperationException("This protocol formatter does not support encoding to stream.");
    }

    @Override
    public Object encodeMessage(String responseReceiverId, Object message)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ProtocolMessage aProtocolMessage = new ProtocolMessage(EProtocolMessageType.MessageReceived, responseReceiverId, message);
            return aProtocolMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void encodeMessage(String responseReceiverId, Object message,
            OutputStream outputSream) throws Exception
    {
        throw new UnsupportedOperationException("This protocol formatter does not support encoding to stream.");
    }

    @Override
    public ProtocolMessage decodeMessage(InputStream readStream)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            throw new UnsupportedOperationException("This protocol formatter does not support decoding from stream.");
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public ProtocolMessage decodeMessage(Object readMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return (ProtocolMessage)readMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.internal.EncoderDecoder;
import eneter.messaging.diagnostic.EneterTrace;


class MonitoredMessagingCustomSerializer implements ISerializer
{

    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (clazz != MonitorChannelMessage.class)
            {
                throw new IllegalArgumentException("Only " + MonitorChannelMessage.class.getSimpleName() + " can be serialized.");
            }

            Object aTemp = dataToSerialize;
            MonitorChannelMessage aMessage = (MonitorChannelMessage)aTemp;

            ByteArrayOutputStream aStream = new ByteArrayOutputStream();
            DataOutputStream aWriter = new DataOutputStream(aStream);

            // Write message type.
            byte aMessageType = (byte)aMessage.MessageType.geValue();
            aWriter.writeByte((byte)aMessageType);

            // Write message data.
            if (aMessage.MessageType == MonitorChannelMessageType.Message)
            {
                if (aMessage.MessageContent == null)
                {
                    throw new IllegalStateException("Message data is null.");
                }

                myEncoderDecoder.write(aWriter, aMessage.MessageContent, myIsLittleEndian);
            }

            return aStream.toByteArray();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(Object serializedData, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (serializedData instanceof byte[] == false &&
                    serializedData instanceof Byte[] == false)
            {
                throw new IllegalArgumentException("Input parameter 'serializedData' is not byte[].");
            }

            if (clazz != MonitorChannelMessage.class)
            {
                throw new IllegalStateException("Data can be deserialized only into" + MonitorChannelMessage.class.getSimpleName());
            }

            MonitorChannelMessage aResult;
            
            byte[] aData = (byte[])serializedData;
            
            ByteArrayInputStream aStream = new ByteArrayInputStream(aData);
            DataInputStream aReader = new DataInputStream(aStream);
            
            // Read type of the message.
            int aRequest = aReader.readByte();
            MonitorChannelMessageType aMessageType = MonitorChannelMessageType.fromInt(aRequest);

            // If it is the message then read data.
            Object aMessageData = null;
            if (aMessageType == MonitorChannelMessageType.Message)
            {
                aMessageData = myEncoderDecoder.read(aReader, myIsLittleEndian);
            }

            aResult = new MonitorChannelMessage(aMessageType, aMessageData);
            return (T)aResult;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private boolean myIsLittleEndian = true;
    private EncoderDecoder myEncoderDecoder = new EncoderDecoder();
}

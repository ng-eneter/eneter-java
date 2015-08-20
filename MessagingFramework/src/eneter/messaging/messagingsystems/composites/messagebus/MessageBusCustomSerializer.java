/**
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2015
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import java.io.*;
import java.nio.charset.Charset;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.internal.EncoderDecoder;
import eneter.messaging.diagnostic.EneterTrace;

class MessageBusCustomSerializer implements ISerializer
{
    public MessageBusCustomSerializer()
    {
        this(true);
    }

    public MessageBusCustomSerializer(boolean isLittleEndian)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myIsLittleEndian = isLittleEndian;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (clazz != MessageBusMessage.class)
            {
                throw new IllegalStateException("Only " + MessageBusMessage.class.getSimpleName() + " can be serialized.");
            }

            Object aTemp = dataToSerialize;
            MessageBusMessage aMessage = (MessageBusMessage)aTemp;

            ByteArrayOutputStream aStream = new ByteArrayOutputStream();
            DataOutputStream aWriter = new DataOutputStream(aStream);
            
            // Write messagebus request.
            byte aRequestType = (byte)aMessage.Request.geValue();
            aWriter.writeByte(aRequestType);

            // Write Id.
            Charset anEncoding = Charset.forName("UTF-8");
            myEncoderDecoder.writePlainString(aWriter, aMessage.Id, anEncoding, myIsLittleEndian);

            // Write message data.
            if (aMessage.Request == EMessageBusRequest.SendRequestMessage ||
                aMessage.Request == EMessageBusRequest.SendResponseMessage)
            {
                if (aMessage.MessageData == null)
                {
                    throw new IllegalStateException("Message data is null.");
                }

                myEncoderDecoder.write(aWriter, aMessage.MessageData, myIsLittleEndian);
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

            if (clazz != MessageBusMessage.class)
            {
                throw new IllegalStateException("Data can be deserialized only into" + MessageBusMessage.class.getSimpleName());
            }

            MessageBusMessage aResult;

            byte[] aData = (byte[])serializedData;

            ByteArrayInputStream aStream = new ByteArrayInputStream(aData);
            DataInputStream aReader = new DataInputStream(aStream);

            // Read message bus request.
            int aRequest = aReader.readByte();
            EMessageBusRequest aMessageBusRequest = EMessageBusRequest.fromInt(aRequest);

            // Read Id
            Charset anEncoding = Charset.forName("UTF-8");
            String anId = myEncoderDecoder.readPlainString(aReader, anEncoding, myIsLittleEndian);

            // Read message data.
            Object aMessageData = null;
            if (aMessageBusRequest == EMessageBusRequest.SendRequestMessage ||
                aMessageBusRequest == EMessageBusRequest.SendResponseMessage)
            {
                aMessageData = myEncoderDecoder.read(aReader, myIsLittleEndian);
            }


            aResult = new MessageBusMessage(aMessageBusRequest, anId, aMessageData);
            return (T)aResult;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private boolean myIsLittleEndian;
    private EncoderDecoder myEncoderDecoder = new EncoderDecoder();
}

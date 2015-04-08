/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import java.io.*;
import java.nio.charset.Charset;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.internal.EncoderDecoder;
import eneter.messaging.diagnostic.EneterTrace;

class BrokerCustomSerializer implements ISerializer
{
    public BrokerCustomSerializer()
    {
        this(true);
    }

    public BrokerCustomSerializer(boolean isLittleEndian)
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
            if (clazz != BrokerMessage.class)
            {
                throw new IllegalStateException("Only " + BrokerMessage.class.getSimpleName() + " can be serialized.");
            }

            Object aTemp = dataToSerialize;
            BrokerMessage aBrokerMessage = (BrokerMessage)aTemp;

            ByteArrayOutputStream aStream = new ByteArrayOutputStream();
            DataOutputStream aWriter = new DataOutputStream(aStream);

            // Write broker request.
            byte aBrokerRequestValue = (byte)aBrokerMessage.Request.geValue();
            aWriter.writeByte(aBrokerRequestValue);

            // Write message types.
            myEncoderDecoder.writeInt32(aWriter, aBrokerMessage.MessageTypes.length, myIsLittleEndian);

            Charset anEncoding = Charset.forName("UTF-8");
            for (String aMessageType : aBrokerMessage.MessageTypes)
            {
                myEncoderDecoder.writePlainString(aWriter, aMessageType, anEncoding, myIsLittleEndian);
            }

            // Write message.
            if (aBrokerMessage.Request == EBrokerRequest.Publish)
            {
                myEncoderDecoder.write(aWriter, aBrokerMessage.Message, myIsLittleEndian);
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
            if (serializedData instanceof byte[] == false)
            {
                throw new IllegalArgumentException("Input parameter 'serializedData' is not byte[].");
            }

            if (clazz != BrokerMessage.class)
            {
                throw new IllegalStateException("Data can be deserialized only into" + BrokerMessage.class.getSimpleName());
            }

            BrokerMessage aResult;

            byte[] aData = (byte[])serializedData;

            ByteArrayInputStream aStream = new ByteArrayInputStream(aData);
            DataInputStream aReader = new DataInputStream(aStream);

            // Read broker request.
            int aBrokerRequestNumber = aReader.readByte();
            EBrokerRequest aBrokerRequest = EBrokerRequest.fromInt(aBrokerRequestNumber);


            // Read number of message types.
            int aNumberOfMessageTypes = myEncoderDecoder.readInt32(aReader, myIsLittleEndian);

            // Read message types.
            Charset anEncoding = Charset.forName("UTF-8");
            String[] aMessageTypes = new String[aNumberOfMessageTypes];
            for (int i = 0; i < aMessageTypes.length; ++i)
            {
                String aMessageType = myEncoderDecoder.readPlainString(aReader, anEncoding, myIsLittleEndian);
                aMessageTypes[i] = aMessageType;
            }

            if (aBrokerRequest == EBrokerRequest.Publish)
            {
                Object aPublishedMessage = myEncoderDecoder.read(aReader, myIsLittleEndian);
                aResult = new BrokerMessage(aMessageTypes[0], aPublishedMessage);
            }
            else
            {
                aResult = new BrokerMessage(aBrokerRequest, aMessageTypes);
            }

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

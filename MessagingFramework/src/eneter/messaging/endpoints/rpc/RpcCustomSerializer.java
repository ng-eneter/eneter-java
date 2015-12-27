package eneter.messaging.endpoints.rpc;

import java.io.*;
import java.nio.charset.Charset;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.internal.EncoderDecoder;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.internal.StringExt;

/**
 * Serializer optimized for RPC.
 * Peroformance of this serializer is optimized for RPC.
 * To increase the performance it serializes RpcMessage (which is internaly used for RPC interaction) into a special byte sequence.
 * It uses underlying serializer to serialize method inrput paramters.
 *
 */
public class RpcCustomSerializer implements ISerializer
{
    /**
     * Constructs the serializer.
     * @param serializer Underlying serializer used to serialize/deserialize method's input parameters.
     */
    public RpcCustomSerializer(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myIsLittleEndian = true;
            myUnderlyingSerializer = serializer;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Serializes data.
     * If the serialized type is RpcMessage then it serializes it into the special sequence
     * otherwise it uses the underlying serializer to serialize it.
     */
    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (clazz == RpcMessage.class)
            {
                RpcMessage anRpcMessage = (RpcMessage)dataToSerialize;
                return serializeRpcMessage(anRpcMessage);
            }
            else
            {
                return myUnderlyingSerializer.serialize(dataToSerialize, clazz);
            }
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
            if (clazz == RpcMessage.class)
            {
                if (serializedData instanceof byte[] == false)
                {
                    throw new IllegalStateException("Failed to deserialize RpcMessage because the input parameter 'serializedData' is not byte[].");
                }
                
                return (T)deserializeRpcMessage((byte[])serializedData);
            }
            else
            {
                return myUnderlyingSerializer.deserialize(serializedData, clazz);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private Object serializeRpcMessage(RpcMessage rpcMessage) throws Exception
    {
        Charset anEncoding = Charset.forName("UTF-8");
        ByteArrayOutputStream aStream = new ByteArrayOutputStream();
        DataOutputStream aWriter = new DataOutputStream(aStream);

        // Write Id of the request.
        myEncoderDecoder.writeInt32(aWriter, rpcMessage.Id, myIsLittleEndian);

        // Write request flag.
        byte aRequestType = (byte)rpcMessage.Request.geValue();
        aWriter.write(aRequestType);

        if (rpcMessage.Request == ERpcRequest.InvokeMethod ||
            rpcMessage.Request == ERpcRequest.RaiseEvent)
        {
            // Write name of the method or name of the event which shall be raised.
            myEncoderDecoder.writePlainString(aWriter, rpcMessage.OperationName, anEncoding, myIsLittleEndian);

            // Write number of input parameters.
            if (rpcMessage.SerializedParams == null)
            {
                myEncoderDecoder.writeInt32(aWriter, 0, myIsLittleEndian);
            }
            else
            {
                myEncoderDecoder.writeInt32(aWriter, rpcMessage.SerializedParams.length, myIsLittleEndian);
                // Write already serialized input parameters.
                for (int i = 0; i < rpcMessage.SerializedParams.length; ++i)
                {
                    myEncoderDecoder.write(aWriter, rpcMessage.SerializedParams[i], myIsLittleEndian);
                }
            }
        }
        else if (rpcMessage.Request == ERpcRequest.SubscribeEvent ||
                 rpcMessage.Request == ERpcRequest.UnsubscribeEvent)
        {
            // Write name of the event which shall be subscribed or unsubcribed.
            myEncoderDecoder.writePlainString(aWriter, rpcMessage.OperationName, anEncoding, myIsLittleEndian);
        }
        else if (rpcMessage.Request == ERpcRequest.Response)
        {
            // Write already serialized return value.
            if (rpcMessage.SerializedReturn != null)
            {
                // Indicate it is not void.
                aWriter.write((byte)1);

                // Wtrite return value.
                myEncoderDecoder.write(aWriter, rpcMessage.SerializedReturn, myIsLittleEndian);
            }
            else
            {
                // Indicate there is no rturn value.
                aWriter.write((byte)0);
            }

            if (!StringExt.isNullOrEmpty(rpcMessage.ErrorType))
            {
                // Indicate the response contains the error message.
                aWriter.write((byte)1);

                // Write error.
                myEncoderDecoder.writePlainString(aWriter, rpcMessage.ErrorType, anEncoding, myIsLittleEndian);
                myEncoderDecoder.writePlainString(aWriter, rpcMessage.ErrorMessage, anEncoding, myIsLittleEndian);
                myEncoderDecoder.writePlainString(aWriter, rpcMessage.ErrorDetails, anEncoding, myIsLittleEndian);
            }
            else
            {
                // Indicate the response does not contain the error message.
                aWriter.write((byte)0);
            }
        }

        return aStream.toByteArray();
     }

    private RpcMessage deserializeRpcMessage(byte[] data) throws Exception
    {
        Charset anEncoding = Charset.forName("UTF-8");
        ByteArrayInputStream aStream = new ByteArrayInputStream(data);
        DataInputStream aReader = new DataInputStream(aStream);

        RpcMessage anRpcMessage = new RpcMessage();

        // Read Id of the request.
        anRpcMessage.Id = myEncoderDecoder.readInt32(aReader, myIsLittleEndian);

        // Read request flag.
        int aRequest = aReader.readByte();
        anRpcMessage.Request = ERpcRequest.fromInt(aRequest);

        if (anRpcMessage.Request == ERpcRequest.InvokeMethod ||
            anRpcMessage.Request == ERpcRequest.RaiseEvent)
        {
            // Read name of the method or name of the event which shall be raised.
            anRpcMessage.OperationName = myEncoderDecoder.readPlainString(aReader, anEncoding, myIsLittleEndian);

            // Read number of input parameters.
            int aNumberParameters = myEncoderDecoder.readInt32(aReader, myIsLittleEndian);

            // Read input parameters.
            anRpcMessage.SerializedParams = new Object[aNumberParameters];
            for (int i = 0; i < anRpcMessage.SerializedParams.length; ++i)
            {
                anRpcMessage.SerializedParams[i] = myEncoderDecoder.read(aReader, myIsLittleEndian);
            }
        }
        else if (anRpcMessage.Request == ERpcRequest.SubscribeEvent ||
                 anRpcMessage.Request == ERpcRequest.UnsubscribeEvent)
        {
            // Read name of the event which shall be subscribed or unsubcribed.
            anRpcMessage.OperationName = myEncoderDecoder.readPlainString(aReader, anEncoding, myIsLittleEndian);
        }
        else if (anRpcMessage.Request == ERpcRequest.Response)
        {
            int aReturnFlag = aReader.readByte();
            if (aReturnFlag == 1)
            {
                anRpcMessage.SerializedReturn = myEncoderDecoder.read(aReader, myIsLittleEndian);
            }

            int anErrorFlag = aReader.readByte();
            if (anErrorFlag == 1)
            {
                anRpcMessage.ErrorType = myEncoderDecoder.readPlainString(aReader, anEncoding, myIsLittleEndian);
                anRpcMessage.ErrorMessage = myEncoderDecoder.readPlainString(aReader, anEncoding, myIsLittleEndian);
                anRpcMessage.ErrorDetails = myEncoderDecoder.readPlainString(aReader, anEncoding, myIsLittleEndian);
            }
        }

        return anRpcMessage;
    }
    
    private ISerializer myUnderlyingSerializer;
    private boolean myIsLittleEndian;
    private EncoderDecoder myEncoderDecoder = new EncoderDecoder();
}

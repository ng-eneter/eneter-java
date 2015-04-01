/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing.internal;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import eneter.messaging.diagnostic.EneterTrace;

/**
 * Internal class for encoding/decoding serialized data.
 * It stores informarmation about used encoding (UTF8 or UTF16) and Little Endian and Big Endian number encoding.
 * It helps to ensure compatibility between various platforms.
 *
 */
public class EncoderDecoder
{
    public void encode(OutputStream writer, Object serializedData)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Encrypt serialized data.
            if (serializedData instanceof String)
            {
                String aSerializedStr = (String)serializedData;
                
                // Note: UTF-16 Big Endian is native Java encoding.
                Charset aCharset = Charset.forName("UTF-16BE");
                ByteBuffer aByteBuffer = aCharset.encode(aSerializedStr);
                byte[] aDataToEncode = aByteBuffer.array();
                
                // Write info that encoded data is UTF16 Big Endian.
                writer.write(STRING_UTF16_BE_ID);
                
                // Encode data.
                writer.write(aDataToEncode);
            }
            else if (serializedData instanceof byte[] ||
                     serializedData instanceof Byte[])
            {
                // Write info, that encoded data is array of bytes.
                writer.write(BYTES_ID);
                
                // Encode data.
                writer.write((byte[])serializedData);
            }
            else
            {
                String anErrorMessage = "Encoding of data failed because the underlying serializer does not serialize to String or byte[].";
                EneterTrace.error(anErrorMessage);
                throw new IllegalAccessError(anErrorMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public Object decode(InputStream reader)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object aDecodedData = null;
            
            // Read type of data.
            int aDataType = reader.read();
            if (aDataType == -1)
            {
                String anErrorMessage = "Decoding of serialized data failed because of unexpected end of stream.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            if (aDataType != STRING_UTF8_ID &&
                aDataType != STRING_UTF16_LE_ID &&
                aDataType != STRING_UTF16_BE_ID &&
                aDataType != BYTES_ID)
            {
                String anErrorMessage = "Decoding of serialized data failed because of incorrect data fromat.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            // Read bytes from the stream until the end.
            ByteArrayOutputStream aDecodedBytes = new ByteArrayOutputStream();
            byte[] aBuffer = new byte[32000];
            int aSize;
            while ((aSize = reader.read(aBuffer)) > 0)
            {
                aDecodedBytes.write(aBuffer, 0, aSize);
            }
            
            if (aDataType == STRING_UTF8_ID)
            {
                aDecodedData = new String(aDecodedBytes.toByteArray(), "UTF-8");
            }
            else if (aDataType == STRING_UTF16_LE_ID)
            {
                aDecodedData = new String(aDecodedBytes.toByteArray(), "UTF-16LE");
            }
            else if (aDataType == STRING_UTF16_BE_ID)
            {
                aDecodedData = new String(aDecodedBytes.toByteArray(), "UTF-16BE");
            }
            else if (aDataType == BYTES_ID)
            {
                aDecodedData = aDecodedBytes.toByteArray();
            }
            else
            {
                String anErrorMessage = "Decoding of serialized data failed because of incorrect data fromat.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            return aDecodedData;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void write(DataOutputStream writer, Object data, boolean isLittleEndianRequested) throws Exception
    {
        if (data instanceof String)
        {
            writeString(writer, (String)data, isLittleEndianRequested);
        }
        else if (data instanceof byte[] ||
                 data instanceof Byte[])
        {
            writeByteArray(writer, (byte[])data, isLittleEndianRequested);
        }
        else
        {
            throw new IllegalStateException("Only byte[] or String is supported.");
        }
    }
    
    public void writeString(DataOutputStream writer, String data, boolean isLittleEndianRequested) throws IOException
    {
        // Write info, that encoded data is string.
        Charset anEncoding = Charset.forName("UTF-8");
        writer.writeByte(STRING_UTF8_ID);
        writePlainString(writer, data, anEncoding, isLittleEndianRequested);
    }
    
    public void writeByteArray(DataOutputStream writer, byte[] data, boolean isLittleEndianRequested) throws IOException
    {
        // Write info, that encoded data is array of bytes.
        writer.writeByte(BYTES_ID);
        writePlainByteArray(writer, data, isLittleEndianRequested);
    }
    
    public Object read(DataInputStream reader, boolean isLittleEndian) throws Exception
    {
        byte aDataType = reader.readByte();
        Object aResult;

        if (aDataType == BYTES_ID)
        {
            aResult = readPlainByteArray(reader, isLittleEndian);
        }
        else
        {
            Charset anEncoding;
            
            if (aDataType == STRING_UTF8_ID)
            {
                anEncoding = Charset.forName("UTF-8");
            }
            else if (aDataType == STRING_UTF16_LE_ID)
            {
                anEncoding = Charset.forName("UTF-16LE");
            }
            else if (aDataType == STRING_UTF16_BE_ID)
            {
                anEncoding = Charset.forName("UTF-16BE");
            }
            else
            {
                throw new IllegalStateException("Unknown encoding type value: " + aDataType);
            }

            aResult = readPlainString(reader, anEncoding, isLittleEndian);
        }

        return aResult;
    }
    
    public void writePlainString(DataOutputStream writer, String data, Charset stringEncoding, boolean isLittleEndianRequested) throws IOException
    {
        ByteBuffer aDataBytes = stringEncoding.encode(data);

        // Note: linit() returns how many bytes are really used.
        writePlainByteArray(writer, aDataBytes.array(), 0, aDataBytes.limit(), isLittleEndianRequested);
    }
    
    public String readPlainString(DataInputStream reader, Charset stringEncoding, boolean isLittleEndian) throws IOException
    {
        byte[] aStringBytes = readPlainByteArray(reader, isLittleEndian);

        String aResult = new String(aStringBytes, stringEncoding);
        return aResult;
    }
    
    public void writePlainByteArray(DataOutputStream writer, byte[] data, boolean isLittleEndianRequested) throws IOException
    {
        writePlainByteArray(writer, data, 0, data.length, isLittleEndianRequested);
    }
    
    private void writePlainByteArray(DataOutputStream writer, byte[] data, int startIndex, int length, boolean isLittleEndianRequested) throws IOException
    {
        // Length of the array.
        writeInt32(writer, length, isLittleEndianRequested);

        // Bytes.
        writer.write(data, startIndex, length);
    }
    
    public byte[] readPlainByteArray(DataInputStream reader, boolean isLittleEndian) throws IOException
    {
        int aLength = readInt32(reader, isLittleEndian);
        byte[] aData = new byte[aLength]; 
        reader.readFully(aData);

        return aData;
    }
    
    public void writeInt32(DataOutputStream writer, int value, boolean isLittleEndianRequested) throws IOException
    {
        // If the endianess of the machine is different than requested endianess then correct it.
        if (isLittleEndianRequested)
        {
            value = switchEndianess(value);
        }

        writer.writeInt(value);
    }
    
    public int readInt32(DataInputStream reader, boolean isLittleEndian) throws IOException
    {
        int aValue = reader.readInt();

        if (isLittleEndian)
        {
            // If the endianess of the machine is same as requested endianess then just write.
            aValue = switchEndianess(aValue);
        }

        return aValue;
    }
    
    private int switchEndianess(int i)
    {
        int anInt = ((i & 0x000000ff) << 24) +
                    ((i & 0x0000ff00) << 8) +
                    ((i & 0x00ff0000) >>> 8) + 
                    ((i & 0xff000000) >>> 24);

        return anInt;
    }
    
    
    private final byte STRING_UTF8_ID = 10;
    private final byte STRING_UTF16_LE_ID = 20;
    private final byte STRING_UTF16_BE_ID = 30;
    private final byte BYTES_ID = 40;
}

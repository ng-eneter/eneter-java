/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.io.*;
import java.security.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.internal.*;

class WebSocketFormatter
{
    //public static byte[] encodeOpenConnectionHttpRequest(URI address, byte[] websocketKey)
    //        throws IOException
    //{
    //    EneterTrace aTrace = EneterTrace.entering();
    //    try
    //    {
    //        if (websocketKey == null || websocketKey.length != 16)
    //        {
    //            throw new IllegalArgumentException("The input parameter websocketKey is not 16 bytes length.");
    //        }
    //
    //        String aKey64baseEncoded = Convert.toBase64String(websocketKey);
    //
    //        String anHttpRequest = String.format("GET %s HTTP/1.1\r\nHost: %s\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: %s\r\nSec-WebSocket-Version: 13\r\n\r\n",
    //            address.getPath(), address.getAuthority(), aKey64baseEncoded);
    //
    //        return anHttpRequest.getBytes("UTF-8");
    //    }
    //    finally
    //    {
    //        EneterTrace.leaving(aTrace);
    //    }
    //}
    
    public static byte[] encodeOpenConnectionHttpRequest(String pathAndQuery, HashMap<String, String> headerFields)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            StringBuilder anHttpRequestBuilder = new StringBuilder();
            anHttpRequestBuilder.append(String.format("GET %s HTTP/1.1\r\n", pathAndQuery));

            Iterator<Map.Entry<String, String>> anIt = headerFields.entrySet().iterator();
            while (anIt.hasNext())
            {
                Map.Entry<String, String> aHeaderField = anIt.next();
                
                anHttpRequestBuilder.append(String.format("%s: %s\r\n", aHeaderField.getKey(), aHeaderField.getValue()));
            }
            
            anHttpRequestBuilder.append("\r\n");
            
            String anHttpRequest = anHttpRequestBuilder.toString(); 

            return anHttpRequest.getBytes("UTF-8");
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodeOpenConnectionHttpResponse(String webSocketKey)
        throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Create response key.
            String aResponseKey = encryptWebSocketKey(webSocketKey);

            // Create the response message.
            String anHttpResponse = String.format("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: %s\r\n\r\n",
                    aResponseKey);

            // Convert response to bytes.
            byte[] aMessageBytes = anHttpResponse.getBytes("UTF-8");

            return aMessageBytes;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodeBinaryMessageFrame(boolean isFinal, byte[] maskingKey, byte[] message)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return encodeMessage(isFinal, (byte)0x02, maskingKey, message);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodeTextMessageFrame(boolean isFinal, byte[] maskingKey, String message)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            byte[] aTextMessage = message.getBytes("UTF-8");
            return encodeMessage(isFinal, (byte)0x01, maskingKey, aTextMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodeContinuationMessageFrame(boolean isFinal, byte[] maskingKey, String message)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Encode the text in UTF8.
            byte[] aTextMessage = message.getBytes("UTF-8");
            
            return encodeMessage(isFinal, (byte)0x00, maskingKey, aTextMessage);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodeContinuationMessageFrame(boolean isFinal, byte[] maskingKey, byte[] message)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return encodeMessage(isFinal, (byte)0x00, maskingKey, message);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodeCloseFrame(byte[] maskingKey, short statusCode)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Convert short to bytes.
            byte[] aShortBytes = { (byte)(statusCode & 0xFF), (byte)((statusCode >> 8) & 0xFF) };
            
            return encodeMessage(true, (byte)0x08, maskingKey, aShortBytes);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodePingFrame(byte[] maskingKey)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: If not data is added then ping has only 2 bytes and the message can stay in a computer buffer
            //       for a very long time. So add some dummy data to the ping message. (websocket protocol supports that)
            byte[] aDummy = { (byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o' };
            return encodeMessage(true, (byte)0x09, maskingKey, aDummy);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodePongFrame(byte[] maskingKey, byte[] pongData)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return encodeMessage(true, (byte)0x0A, maskingKey, pongData);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    public static byte[] encodeMessage(boolean isFinal, byte opCode, byte[] maskingKey, byte[] payload)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (maskingKey != null && maskingKey.length != 4)
            {
                throw new IllegalArgumentException("The input parameter maskingKey must be null or must have length 4.");
            }

            ByteArrayOutputStream aBuffer = new ByteArrayOutputStream();
            try
            {
                DataOutputStream aWriter = new DataOutputStream(aBuffer);
                    
                byte aFirstByte = (byte)(((isFinal) ? 0x80 : 0x00) | opCode);
                aWriter.writeByte(aFirstByte);

                byte aLengthIndicator = (byte)((payload == null) ? 0 : (payload.length <= 125) ? payload.length : (payload.length <= 0xFFFF) ? 126 : 127);

                byte aSecondByte = (byte)(((maskingKey != null) ? 0x80 : 0x00) | aLengthIndicator);
                aWriter.writeByte(aSecondByte);

                if (aLengthIndicator == 126)
                {
                    // Next 2 bytes indicate the length of data.
                    
                    // Note: Java uses big endian. It is same as websocket
                    //       protocol requires. So no conversion is needed.
                    aWriter.writeShort(payload.length);
                }
                else if (aLengthIndicator == 127)
                {
                    // Next 8 bytes indicate the length of data.
                    
                    // Note: Java uses big endian. It is same as websocket
                    //       protocol requires. So no conversion is needed.
                    aWriter.writeLong(payload.length);
                }

                if (maskingKey != null)
                {
                    aWriter.write(maskingKey);
                }

                if (payload != null)
                {
                    if (maskingKey != null)
                    {
                        for (int i = 0; i < payload.length; ++i)
                        {
                            aWriter.writeByte((byte)(payload[i] ^ maskingKey[i % 4]));
                        }
                    }
                    else
                    {
                        aWriter.write(payload);
                    }
                }

                return aBuffer.toByteArray();
            }
            finally
            {
                aBuffer.close();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public static void decodeOpenConnectionHttpRequest(InputStream inputStream,
            HashMap<String, String> regExResult,
            HashMap<String, String> headerFields)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Read incoming http message.
            String anHttp = readHttp(inputStream);
            
            // Parse it as the request opening connection.
            Matcher aParser = myHttpOpenConnectionRequest.matcher(anHttp);
            
            // Get fields of interest.
            int aLineIdx = 0;
            while (aParser.find())
            {
                String aGroup = aParser.group();
                
                // If it is not the last group indicating the end of http.
                if (!aGroup.equals("\r\n"))
                {
                    // If we are at the first line then get the path.
                    if (aLineIdx == 0)
                    {
                        regExResult.put("path", aParser.group(2));
                        regExResult.put("query", aParser.group(4)); 
                    }
                    else
                    {
                        String aKey = aParser.group(6);
                        if (!StringExt.isNullOrEmpty(aKey))
                        {
                            String aValue = aParser.group(7);
                            headerFields.put(aKey, aValue);
                        }
                    }
                }
                
                ++aLineIdx;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static HashMap<String, String> decodeOpenConnectionHttpResponse(InputStream inputStream)
            throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Read incoming http message.
            String anHttp = readHttp(inputStream);
            
            // Parse it as the response for opening connection.
            Matcher aParser = myHttpOpenConnectionResponse.matcher(anHttp);
            
            // Get fields of interest.
            HashMap<String, String> aFields = new HashMap<String, String>();
            int aLineIdx = 0;
            while (aParser.find())
            {
                String aGroup = aParser.group();
                
                // If it is not the last group indicating the end of http.
                if (!aGroup.equals("\r\n"))
                {
                    // If we are at the first line then get the path.
                    if (aLineIdx == 0)
                    {
                        aFields.put("Code", aParser.group(2));
                    }
                    else
                    {
                        String aKey = aParser.group(4);
                        if (!StringExt.isNullOrEmpty(aKey))
                        {
                            String aValue = aParser.group(5);
                            aFields.put(aKey, aValue);
                        }
                    }
                }
                
                ++aLineIdx;
            }

            return aFields;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static WebSocketFrame decodeFrame(InputStream inputStream) throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                DataInputStream aReader = new DataInputStream(inputStream);

                // Read the first 2 bytes.
                byte[] aFirst2Bytes = new byte[2];
                aReader.readFully(aFirst2Bytes, 0, aFirst2Bytes.length);

                // Get if final.
                boolean anIsFinal = (aFirst2Bytes[0] & 0x80) != 0;

                // Get opcode.
                EFrameType aFrameType = EFrameType.getEnum(aFirst2Bytes[0] & 0xF);

                // Get if masked.
                boolean anIsMasked = (aFirst2Bytes[1] & 0x80) != 0;

                // Get the message length.
                int aMessageLength = aFirst2Bytes[1] & 0x7F;
                if (aMessageLength == 126)
                {
                    // The length is encoded in next 2 bytes (16 bits).
                    int aLength = aReader.readUnsignedShort();

                    // Note: Websockets are in Big Endian. It is same as Java uses.
                    //       So no further conversion is needed.
                    
                    aMessageLength = aLength;
                }
                else
                if (aMessageLength == 127)
                {
                    // The length is encoded in next 8 bytes (64 bits).
                    long aLength = aReader.readLong();

                    // Note: Websockets are in Big Endian. It is same as Java uses.
                    //       So no further conversion is needed.

                    aMessageLength = (int)aLength;
                }

                // Get mask bytes.
                byte[] aMaskBytes = null;
                if (anIsMasked)
                {
                    aMaskBytes = new byte[4];
                    aReader.readFully(aMaskBytes);
                }

                // Get the message data.
                byte[] aMessageData = new byte[aMessageLength];
                aReader.readFully(aMessageData);

                // If mask was used then unmask data.
                if (anIsMasked)
                {
                    for (int i = 0; i < aMessageData.length; ++i)
                    {
                        aMessageData[i] = (byte)(aMessageData[i] ^ aMaskBytes[i % 4]);
                    }
                }

                WebSocketFrame aFrame = new WebSocketFrame(aFrameType, anIsMasked, aMessageData, anIsFinal);

                return aFrame;
            }
            catch (EOFException err)
            {
                // End of the stream.
                return null;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static String encryptWebSocketKey(String webSocketKeyBase64)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        String anEncryptedKey = webSocketKeyBase64 + myWebSocketId;

        // Calculate SHA
        byte[] anEncryptedKeyBytes = anEncryptedKey.getBytes("US-ASCII");
        MessageDigest aSHA1 = MessageDigest.getInstance("SHA-1");
        aSHA1.update(anEncryptedKeyBytes);
        byte[] anEncodedResponseKey = aSHA1.digest();
        
        // Convert to base 64bits digits.
        String anEncryptedKeyBase64 = Convert.toBase64String(anEncodedResponseKey);

        return anEncryptedKeyBase64;
    }
    
    private static String readHttp(InputStream inputStream) throws IOException
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream anHttpHeaderBuffer = new ByteArrayOutputStream();
            try
            {
                int aStopFlag = 4;
                while (aStopFlag > 0)
                {
                    int aValue = inputStream.read();
                    if (aValue == -1)
                    {
                        String anErrorMessage = "End of stream during reading HTTP header.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }
                    
                    // Store the received byte - belonging to the header.
                    anHttpHeaderBuffer.write(aValue);
                    
                    // Detect sequence /r/n/r/n - the end of the HTTP header.
                    if (aValue == 13 && (aStopFlag == 4 || aStopFlag == 2) ||
                       (aValue == 10 && (aStopFlag == 3 || aStopFlag == 1)))
                    {
                        --aStopFlag;
                    }
                    else
                    {
                        aStopFlag = 4;
                    }
                }
                
                byte[] anHttpHeaderBytes = anHttpHeaderBuffer.toByteArray();
                String anHttpHeaderStr = new String(anHttpHeaderBytes, "UTF-8"); 
                
                return anHttpHeaderStr;
            }
            finally
            {
                anHttpHeaderBuffer.close();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private static final Pattern myHttpOpenConnectionRequest = Pattern.compile(
            "(^GET\\s([^\\s\\?]+)(\\?([^\\s]+))?\\sHTTP\\/1\\.1\\r\\n)|" +
            "(([^:\\r\\n]+):\\s([^\\r\\n]+)\\r\\n)|" +
            "\\r\\n",
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern myHttpOpenConnectionResponse = Pattern.compile(
            "(^HTTP/1\\.1\\s([\\d]+)\\s.*\\r\\n)|" +
            "(([^:\\r\\n]+):\\s([^\\r\\n]+)\\r\\n)|" +
            "(\\r\\n)",        
            Pattern.CASE_INSENSITIVE); 
            
            
    
    private static final String myWebSocketId = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    
}

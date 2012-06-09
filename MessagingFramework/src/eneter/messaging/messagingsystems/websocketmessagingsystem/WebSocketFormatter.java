package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.Convert;
import eneter.net.system.StringExt;

class WebSocketFormatter
{

    
    public static HashMap<String, String> decodeOpenConnectionHttpRequest(InputStream inputStream)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Read incoming http message.
            String anHttpRequest = readHttp(inputStream);
            
            // Parse it.
            Matcher aParser = myHttpOpenConnectionRequest.matcher(anHttpRequest);
            
            // Get fields of interest.
            HashMap<String, String> aFields = new HashMap<String, String>();
            int aLineIdx = 0;
            while (http.find())
            {
                String aGroup = http.group();
                
                // If it is not the last group indicating the end of http.
                if (!aGroup.equals("\r\n"))
                {
                    // If we are at the first line then get the path.
                    if (aLineIdx == 0)
                    {
                        aHeaderFields.put("Path", http.group(2));
                    }
                    else
                    {
                        String aKey = http.group(4);
                        if (!StringExt.isNullOrEmpty(aKey))
                        {
                            aHeaderFields.put(aKey, http.group(5));
                        }
                    }
                }
                
                ++aLineIdx;
            }

            return aHeaderFields;
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
    
    public static String encryptWebSocketKey(String webSocketKeyBase64) throws NoSuchAlgorithmException
    {
        String anEncryptedKey = webSocketKeyBase64 + myWebSocketId;

        // Calculate SHA
        ByteBuffer aByteBuffer = Charset.forName("US-ASCII").encode(anEncryptedKey);
        MessageDigest aSHA1 = MessageDigest.getInstance("SHA-1");
        aSHA1.update(aByteBuffer);
        byte[] anEncodedResponseKey = aSHA1.digest();
        
        // Convert to base 64bits digits.
        String anEncryptedKeyBase64 = Convert.toBase64String(anEncodedResponseKey);

        return anEncryptedKeyBase64;
    }
    
    private static String readHttp(InputStream inputStream) throws Exception
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

                // Split the http message to lines.
                String[] aResult = anHttpHeaderStr.split("\r\n");
                
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
            "(^GET\\s([^\\s\\?]+)(\\?[^\\s]+)?\\sHTTP\\/1\\.1\\r\\n)|" +
            "(([^:\\r\\n]+):\\s([^\\r\\n]+)\\r\\n)+" +
            "\\r\\n",
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern myHttpOpenConnectionResponse = Pattern.compile(
            "HTTP/1\\.1\\s([\\d]+)\\s.*\\r\\n" +
            "(([^:\\r\\n]+):\\s([^\\r\\n]+)\\r\\n)+" +
            "\\r\\n",        
            Pattern.CASE_INSENSITIVE); 
            
            
    
    private static final String myWebSocketId = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    
}

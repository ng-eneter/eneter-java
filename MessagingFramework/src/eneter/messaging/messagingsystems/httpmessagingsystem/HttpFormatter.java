package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.StringExt;

class HttpFormatter
{
   
    public static void decodeHttpRequest(InputStream inputStream,
            HashMap<String, String> regExResult,
            HashMap<String, String> headerFields)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Read HTTP header.
            // Note: 2 clrf means \r\n\r\n
            String anHttpHeaderStr = readHttp(inputStream, 2);

            // Parse HTTP header.
            Matcher aParser = myHttpRequestRegex.matcher(anHttpHeaderStr);
            
            // Get header fields.
            int aLineIdx = 0;
            while (aParser.find())
            {
                String aGroup = aParser.group();
                
                // If it is not the last group indicating the end of http.
                if (!aGroup.equals("\r\n"))
                {
                    // If we are at the first line then get the path.
                 // If we are at the first line then get the path.
                    if (aLineIdx == 0)
                    {
                        regExResult.put("path", aParser.group(3));
                        regExResult.put("query", aParser.group(5)); 
                    }
                    else
                    {
                        String aKey = aParser.group(7);
                        if (!StringExt.isNullOrEmpty(aKey))
                        {
                            String aValue = aParser.group(8);
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

    public static byte[] decodeChunk(InputStream inputStream)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Read HTTP chunk line.
            // Note: 1 clrf means \r\n
            String aChunkLine = readHttp(inputStream, 1);
            
            Matcher aParser = myHttpChunkRegex.matcher(aChunkLine);
            if (!aParser.matches())
            {
                throw new IllegalStateException("Incorrect format of line identifying the size of the http chunk.");
            }
            
            // Extract size of the chunk.
            String aSizeHex = aParser.group(1);
            if (aSizeHex == null)
            {
                throw new IllegalStateException("Incorrect format of line identifying the size of the http chunk.");
            }
            int aChunkSize = Integer.parseInt(aSizeHex, 16);

            // Read the chunk.
            // Note: Do not enclose the BinaryReader with using because it will close the stream!!!
            DataInputStream aReader = new DataInputStream(inputStream);
            byte[] aChunkData = new byte[aChunkSize];
            aReader.readFully(aChunkData, 0, aChunkData.length);
            
            // Chunk data is folowed by \r\n - so read it to position the stream behind this chunk.
            byte[] aFooter = new byte[2];
            aReader.readFully(aFooter, 0, aFooter.length);

            if (aFooter[0] != (byte)'\r' && aFooter[1] != (byte)'\n')
            {
                throw new IllegalStateException("Http chunk data is not followed by \\r\\n.");
            }

            return aChunkData;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodeError(int errorCode)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return encodeResponse(errorCode, null, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public static byte[] encodeResponse(int statusCode, byte[] responseData, boolean isChunk)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Response status line.
            StringBuilder aHeaderBuilder = new StringBuilder();
            aHeaderBuilder.append(String.format("HTTP/1.1 %d %s\r\n", statusCode, myReasonPhrases.get(statusCode)));

            // If response data is present and can be sent.
            boolean isData = statusCode >= 200 && statusCode != 204 && statusCode != 304 &&
                          responseData != null && responseData.length > 0;

            HashMap<String, String> aHeaderFields = new HashMap<String, String>();
            aHeaderFields.put("Content-Type", "application/octet-stream");

            if (isData)
            {
                // If it is not the chunk.
                if (!isChunk)
                {
                    aHeaderFields.put("Content-Length", Integer.toString(responseData.length));
                }
                else
                {
                    // Ensure 'Content-Length' is not among header fields.
                    aHeaderFields.remove("Content-Length");

                    // Ensure 'Transfer-encoding: chunked'
                    aHeaderFields.put("Transfer-encoding", "chunked");
                }
            }

            // Add header fields.
            for (Entry<String, String> aHeaderField : aHeaderFields.entrySet())
            {
                aHeaderBuilder.append(String.format("%s: %s\r\n", aHeaderField.getKey(), aHeaderField.getValue()));
            }

            // End of http header.
            aHeaderBuilder.append("\r\n");
            
            // Convert http header to bytes.
            String anHttpHeader = aHeaderBuilder.toString();
            byte[] aHeaderBytes = anHttpHeader.getBytes("UTF-8");

            // If response data is available.
            if (isData)
            {
                byte[] aMessageBytes = new byte[aHeaderBytes.length + responseData.length];
                System.arraycopy(aHeaderBytes, 0, aMessageBytes, 0, aHeaderBytes.length);
                System.arraycopy(responseData, 0, aMessageBytes, aHeaderBytes.length, responseData.length);
                
                return aMessageBytes;
            }

            return aHeaderBytes;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private static String readHttp(InputStream inputStream, int numberOfClrf)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Read HTTP header.
            ByteArrayOutputStream anHttpHeaderBuffer = new ByteArrayOutputStream();
            try
            {
                int aStopFlag = numberOfClrf * 2;
                while (aStopFlag != 0)
                {
                    int aValue = inputStream.read();
                    if (aValue == -1)
                    {
                        String anErrorMessage = "End of stream during reading HTTP header.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }

                    // Store the received byte - belonging to the header.
                    anHttpHeaderBuffer.write((byte)aValue);

                    // Detect sequence /r/n/r/n - the end of the HTTP header.
                    if (aValue == 13 && (aStopFlag == 4 || aStopFlag == 2) ||
                       (aValue == 10 && (aStopFlag == 3 || aStopFlag == 1)))
                    {
                        --aStopFlag;
                    }
                    else
                    {
                        aStopFlag = numberOfClrf * 2;
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
    
    
    private static final Pattern myHttpRequestRegex = Pattern.compile(
            "(^([A-Za-z]+)\\s([^\\s\\?]+)(\\?([^\\s]+))?\\sHTTP\\/1\\.1\\r\\n)|" +
            "(([^:\\r\\n]+):\\s([^\\r\\n]+)\\r\\n)|" +
            "\\r\\n",
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern myHttpChunkRegex = Pattern.compile(
            "^([\\w]+)\\s[.]?\\r\\n",
            Pattern.CASE_INSENSITIVE);
    
    private static HashMap<Integer, String> myReasonPhrases = new HashMap<Integer, String>();

    // Static constructor.
    static
    {
        myReasonPhrases.put(200, "Continue");
        myReasonPhrases.put(400, "Bad Request");
        myReasonPhrases.put(404, "Not Found");
    }
}

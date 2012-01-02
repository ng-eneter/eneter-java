package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.*;
import java.net.*;

import eneter.messaging.diagnostic.*;

class HttpClient
{
    public static void sendOnewayRequest(URL url, byte[] content)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            HttpURLConnection aConnection = (HttpURLConnection)url.openConnection();
            try
            {
                aConnection.setDoOutput(true);
                aConnection.setRequestMethod("POST");

                // Write the message to the stream.
                OutputStream aSender = aConnection.getOutputStream();
                aSender.write(content);
                
                // Fire the message.
                // Note: requesting the response code will fire the message.
                int aResponseCode = aConnection.getResponseCode();
                if (aResponseCode != 200)
                {
                    String aResponseMessage = "HTTP error: " + aResponseCode + " " + aConnection.getResponseMessage();
                    throw new IllegalStateException(aResponseMessage);
                }
            }
            finally
            {
                if (aConnection != null)
                {
                    aConnection.disconnect();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    public static byte[] sendRequest(URL url, byte[] content)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            HttpURLConnection aConnection = (HttpURLConnection)url.openConnection();
            try
            {
                aConnection.setDoOutput(true);
                aConnection.setRequestMethod("POST");

                // Write the message to the stream.
                OutputStream aSender = aConnection.getOutputStream();
                aSender.write(content);
                
                // Fire the message.
                // Note: requesting the response code will fire the message.
                int aResponseCode = aConnection.getResponseCode();
                if (aResponseCode != 200)
                {
                    String aResponseMessage = "HTTP error: " + aResponseCode + " " + aConnection.getResponseMessage();
                    throw new IllegalStateException(aResponseMessage);
                }
                
                int aResponseContentSize = aConnection.getHeaderFieldInt("content-length", 0);
                if (aResponseContentSize > 0)
                {
                    byte[] aResponseContent = new byte[aResponseContentSize];
                    InputStream aResponseStream = aConnection.getInputStream();
                    
                    // Note: I assume, in case of ENETER communication, all response data should be available at once.
                    aResponseStream.read(aResponseContent);
                    
                    return aResponseContent;
                }
                
                return new byte[0];
            }
            finally
            {
                if (aConnection != null)
                {
                    aConnection.disconnect();
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
}

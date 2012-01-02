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
                
                InputStream aResponseStream = aConnection.getInputStream();
                ByteArrayOutputStream aResponseContentStream = new ByteArrayOutputStream();
                int aSize = 0;
                byte[] aBuffer = new byte[32764];
                while ((aSize = aResponseStream.read(aBuffer)) != -1)
                {
                    aResponseContentStream.write(aBuffer, 0, aSize);
                }
                
                return aResponseContentStream.toByteArray();
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

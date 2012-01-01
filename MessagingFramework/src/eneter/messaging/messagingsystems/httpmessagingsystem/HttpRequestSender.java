package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.OutputStream;
import java.net.*;

import eneter.messaging.diagnostic.*;

class HttpRequestSender
{
    public static void send(URL url, byte[] content)
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
                    String aResponseMessage = aConnection.getResponseMessage();
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
}

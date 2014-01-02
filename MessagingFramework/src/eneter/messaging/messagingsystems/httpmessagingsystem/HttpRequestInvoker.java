/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.*;
import java.net.*;

import eneter.messaging.dataprocessing.streaming.internal.StreamUtil;
import eneter.messaging.diagnostic.*;

class HttpRequestInvoker
{
    public static byte[] invokeGetRequest(URL url) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: If the protocol is https, then HttpsURLConnection is automatically created.
            //       HttpsURLConnection is derived from HttpURLConnection.
            HttpURLConnection aConnection = (HttpURLConnection)url.openConnection();
            try
            {
                aConnection.setDoOutput(false);
                aConnection.setRequestMethod("GET");

                // Fire the message.
                // Note: requesting the response code will fire the message.
                int aResponseCode = aConnection.getResponseCode();
                if (aResponseCode != 200)
                {
                    String aResponseMessage = "HTTP error: " + aResponseCode + " " + aConnection.getResponseMessage();
                    throw new IllegalStateException(aResponseMessage);
                }
                
                InputStream aResponseStream = aConnection.getInputStream();
                return StreamUtil.readToEnd(aResponseStream);
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
    
    public static byte[] invokePostRequest(URL url, byte[] content)
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
                return StreamUtil.readToEnd(aResponseStream);
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

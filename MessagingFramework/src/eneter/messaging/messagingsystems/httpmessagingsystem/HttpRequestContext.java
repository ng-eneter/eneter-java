package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.OutputStream;
import java.net.*;


import eneter.messaging.diagnostic.EneterTrace;

class HttpRequestContext
{
    public HttpRequestContext(URI uri, String httpMethod, InetAddress remoteEndPoint, byte[] requestMessage, OutputStream responseStream)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUri = uri;
            myHttpMethod = httpMethod;
            myRemoteEndPoint = (remoteEndPoint != null) ? remoteEndPoint.toString() : "";
            myResponseStream = responseStream;
            myRequestMessage = requestMessage;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public URI getUri()
    {
        return myUri;
    }
    
    public String getHttpMethod()
    {
        return myHttpMethod;
    }
    
    public String getRemoteEndPoint()
    {
        return myRemoteEndPoint;
    }

    public byte[] getRequestMessage()
    {
        return myRequestMessage;
    }
    
    public void response(byte[] message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myIsResponded)
            {
                throw new IllegalStateException("It is not allowed to send more than one response message per request message.");
            }

            // Encode the http response.
            byte[] aResponse = HttpFormatter.encodeResponse(200, message, false);

            // Send the message.
            myResponseStream.write(aResponse, 0, aResponse.length);

            myIsResponded = true;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public void responseError(int statusCode) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myIsResponded)
            {
                throw new IllegalStateException("It is not allowed to send more than one response message per request message.");
            }

            // Encode the http response.
            byte[] aResponse = HttpFormatter.encodeResponse(statusCode, null, false);

            // Send the message.
            myResponseStream.write(aResponse, 0, aResponse.length);

            myIsResponded = true;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public boolean isResponded()
    {
        return myIsResponded;
    }
    
    
    private URI myUri;
    private String myHttpMethod;
    private String myRemoteEndPoint;
    private boolean myIsResponded;
    private OutputStream myResponseStream;
    private byte[] myRequestMessage;
}

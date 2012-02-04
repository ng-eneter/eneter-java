/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.URI;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.ErrorHandler;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IListenerProvider;
import eneter.net.system.IMethod1;

/**
 * HTTP policy server needed for the communication with Silverlight applications.
 * 
 * The policy server is required by Silverlight for the communication via HTTP or TCP.
 * (See also TcpPolicyServer.)
 * Windows Phone 7 (based on Silverlight 3 too) does not require the policy server.
 * <br/><br/>
 * The HTTP policy server is a special service listening to the HTTP root address. When it receives the
 * request with the path '/clientaccesspolicy.xml', it returns the content of the policy file.
 * <br/><br/>
 * Silverlight automatically uses this service before an HTTP request is invoked.
 * If the Silverlight application invokes the HTTP request (e.g. http://127.0.0.1/MyService/),
 * Silverlight first sends the request on the root (i.e. http://127.0.0.1/) and expects the policy file.
 * If the policy server is not there or the content of the policy file does not allow the communication,
 * the original HTTP request is not performed.
 *
 */
public class HttpPolicyServer
{
    /**
     * Constructs the Http policy server.
     * 
     * @param httpRootAddress root Http address. E.g. if the serivice has the address http://127.0.0.1/MyService/, the root
     *            address will be http://127.0.0.1/.
     * @throws Exception
     */
    public HttpPolicyServer(String httpRootAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            setPolicyXml(getSilverlightDefaultPolicyXml());
            
            // Get the http address with the path to the policy xml.
            URI aUri = new URI(httpRootAddress);
            String aPolicyXmlHttp = aUri.getScheme() + "://" + aUri.getAuthority() + myPolicyXmlPath;
            
            // Subscribe to listen to the xml policy path.
            myHttpListenerProvider = new HttpListenerProvider(aPolicyXmlHttp);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Gets the policy xml.
     * @return
     */
    public String getPolicyXml()
    {
        return myPolicyXml;
    }
    
    /**
     * Sets the policy xml.
     * 
     * @param policyXml
     * @throws Exception
     */
    public void setPolicyXml(String policyXml) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myHttpResponse = buildHttpResponse(policyXml);
            myPolicyXml = policyXml;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Returns true, if this instance of policy server is listening to requests.
     * @return
     * @throws Exception
     */
    public boolean isListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myHttpListenerProvider.isListening();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Starts the policy server.
     * 
     * It starts the thread listening to HTTP requests and responding the policy file.
     * 
     * @throws Exception
     */
    public void startPolicyServer() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myHttpListenerProvider.startListening(myHandleConnection);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Stops the policy server.
     * 
     * It stops the listening and responding for requests.
     */
    public void stopPolicyServer()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myHttpListenerProvider.stopListening();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * It is called if the request to http://address/clientaccesspolicy.xml is received.
     * It responses the policy xml.
     * @param tcpClient
     * @throws Exception
     */
    private void handleConnection(Socket tcpClient) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            tcpClient.getOutputStream().write(myHttpResponse);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private String getSilverlightDefaultPolicyXml()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            StringBuilder builder = new StringBuilder();

            builder.append("<?xml version=\"1.0\" encoding =\"utf-8\"?>");
            builder.append("<access-policy>");
            builder.append("  <cross-domain-access>");
            builder.append("    <policy>");
            builder.append("      <allow-from>");
            builder.append("        <domain uri=\"*\" />");
            builder.append("      </allow-from>");
            builder.append("      <grant-to>");
            builder.append("        <resource path=\"/\" include-subpaths=\"true\" />");
            builder.append("      </grant-to>");
            builder.append("    </policy>");
            builder.append("  </cross-domain-access>");
            builder.append("</access-policy>");

            return builder.toString();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private byte[] buildHttpResponse(String policyXml) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ByteArrayOutputStream aBuffer = new ByteArrayOutputStream();
            DataOutputStream aWriter = new DataOutputStream(aBuffer);
            aWriter.writeBytes("HTTP/1.1 200 OK\r\n");
            aWriter.writeBytes("Content-Type: \r\n");
            aWriter.writeBytes("Content-Length: " + policyXml.length() + "\r\n\r\n");
            aWriter.writeBytes(policyXml);
            
            return aBuffer.toByteArray();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private IListenerProvider myHttpListenerProvider;
    private String myPolicyXmlPath = "/clientaccesspolicy.xml";
    private String myHttpRootAddress;
    private byte[] myHttpResponse;
    private String myPolicyXml;
    
    private IMethod1<Socket> myHandleConnection = new IMethod1<Socket>()
    {
        @Override
        public void invoke(Socket x) throws Exception
        {
            handleConnection(x);
        }
    };
    
    private String TracedObject()
    {
        return "HttpPolicyServer '" + myHttpRootAddress + "' ";
    }
}

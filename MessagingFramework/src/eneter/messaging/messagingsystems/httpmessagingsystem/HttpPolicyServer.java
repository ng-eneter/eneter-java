package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.URI;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.ErrorHandler;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IListenerProvider;
import eneter.net.system.IMethod1;

public class HttpPolicyServer
{
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
    
    public String getPolicyXml()
    {
        return myPolicyXml;
    }
    
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

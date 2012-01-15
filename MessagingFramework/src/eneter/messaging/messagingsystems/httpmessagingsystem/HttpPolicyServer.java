package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.ErrorHandler;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpListenerProvider;
import eneter.net.system.IMethod1;

public class HttpPolicyServer
{
    public HttpPolicyServer(String httpRootAddress) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            URL aUrl;
            try
            {
                aUrl = new URL(httpRootAddress);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            catch (Error err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.InvalidUriAddress, err);
                throw err;
            }
            
            setPolicyXml(getSilverlightDefaultPolicyXml());
            
            int aPort = (aUrl.getPort() != -1) ? aUrl.getPort() : aUrl.getDefaultPort();
            myTcpListenerProvider = new TcpListenerProvider(InetAddress.getByName(aUrl.getHost()), aPort);
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

    public boolean isListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return myTcpListenerProvider.isListening();
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
                myTcpListenerProvider.startListening(myHandleConnection);
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
            myTcpListenerProvider.stopListening();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void handleConnection(Socket tcpClient) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Source stream.
            InputStream anInputStream = tcpClient.getInputStream();

            Charset aCharset = Charset.forName("UTF-8");
            
            String aReceivedRequest = "";
            int aSize = 0;
            byte[] aBuffer = new byte[512];
            while (aReceivedRequest.length() < myPolicyRequestString.length() &&
                    (aSize = anInputStream.read(aBuffer, 0, aBuffer.length)) > 0)
            {
                aReceivedRequest += new String(aBuffer, 0, aSize, aCharset);
            }

            // If it is the policy request then return the policy xml
            if (aReceivedRequest.equals(myPolicyRequestString))
            {
                tcpClient.getOutputStream().write(myHttpResponse);
            }
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
    
    private TcpListenerProvider myTcpListenerProvider;
    private String myPolicyRequestString = "GET /clientaccesspolicy.xml";
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

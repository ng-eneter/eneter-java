/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import eneter.messaging.diagnostic.*;
import eneter.net.system.IMethod1;

/**
 * TCP policy server needed for the communication with Silverlight applications.
 * 
 * The policy server is required by Silverlight for the communication via HTTP or TCP.
 * (See also HttpPolicyServer.)<br/>
 * <br/>
 * The TCP policy server is a special service listening on the port 943 (by default for all Ip adresses).
 * When it receives &lt;policy-file-request/&gt; request, it returns the content of the policy file.
 * <br/><br/>
 * Silverlight automatically uses this service before the TCP connection is created.
 * If a Silverlight application wants to open the TCP connection,
 * Silverlight first sends the request on the port 943 and expects the policy file.
 * If the policy server is not there or the content of the policy file does not allow
 * the communication, the Tcp connection is not created.
 *
 */
public class TcpPolicyServer
{
    /**
     * Constructs the TCP policy server providing the policy file on the port 943 for all IP addresses.
     * @throws Exception
     */
    public TcpPolicyServer() throws Exception
    {
        this(InetAddress.getByName("0.0.0.0"));
    }
    
    /**
     * Constructs the TCP policy server providing the policy file on the port 943 for the given IP address.
     * @param ipAddress
     */
    public TcpPolicyServer(InetAddress ipAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myPolicyXml = getSilverlightDefaultPolicyXml();
            
            InetSocketAddress aSocketAddress = new InetSocketAddress(ipAddress, 943);
            myTcpListenerProvider = new TcpListenerProvider(aSocketAddress);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Gets policy xml.
     * @return
     */
    public String getPolicyXml()
    {
        return myPolicyXml;
    }
    
    /**
     * Sets policy xml.
     * @param policyXml
     */
    public void setPolicyXml(String policyXml)
    {
        myPolicyXml = policyXml;
    }
 
    /**
     * Returns true, if this instance of policy server is listening to requests.
     * @return
     */
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
    
    /**
     * Starts the policy server.
     * 
     * It starts the thread listening to requests on port 943 and responding the policy XML.
     * @throws Exception
     */
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
                aReceivedRequest += new String(aBuffer, 0, aSize, "UTF-8");
            }

            // If it is the policy request then return the policy xml
            if (aReceivedRequest.equals(myPolicyRequestString))
            {
                ByteBuffer aBuf = aCharset.encode(myPolicyXml);
                
                // Note: Method array() returns the reference to the internal array, that can
                //       be longer than amount of data.
                //       The actual size of data is returned from limit().
                tcpClient.getOutputStream().write(aBuf.array(), 0, aBuf.limit());
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
            builder.append("        <socket-resource port=\"4502-4532\" protocol=\"tcp\" />");
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
    
    private TcpListenerProvider myTcpListenerProvider;
    private final String myPolicyRequestString = "<policy-file-request/>";
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
        return "TcpPolicyServer ";
    }
}

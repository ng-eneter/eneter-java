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

public class TcpPolicyServer
{
    public TcpPolicyServer() throws Exception
    {
        this(InetAddress.getByName("0.0.0.0"));
    }
    
    
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
    
    public String getPolicyXml()
    {
        return myPolicyXml;
    }
    
    public void setPolicyXml(String policyXml)
    {
        myPolicyXml = policyXml;
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

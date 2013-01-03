package calculator;

import java.io.*;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.dataprocessing.serializing.RsaDigitalSignatureSerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.net.system.EventHandler;

public class Program
{
    public static class MyRequestMsg
    {
        public int Number1;
        public int Number2;
    }
    
    
    // Receiver receiving MyResponseMsg and responding MyRequestMsg
    private static IDuplexTypedMessageReceiver<Integer, MyRequestMsg> myReceiver;

    public static void main(String[] args) throws Exception
    {
        // Start the TCP Policy server.
        // Note: Silverlight requests the policy xml to check if the connection
        //       can be established.
        TcpPolicyServer aPolicyServer = new TcpPolicyServer();
        aPolicyServer.startPolicyServer();
        
        // Digitally signed messages.
        CertificateFactory aCertificateFactory = CertificateFactory.getInstance("X.509");
        FileInputStream aCertificateStream = new FileInputStream("d:/EneterSigner.cer");
        X509Certificate aPublicCertificate = (X509Certificate) aCertificateFactory.generateCertificate(aCertificateStream);
        
        File aPrivateKeyFile = new File("d:/EneterSigner.pk8");
        BufferedInputStream aBufferedPrivateKey = new BufferedInputStream(new FileInputStream(aPrivateKeyFile));
        byte[] aPrivateKeyBytes = new byte[(int)aPrivateKeyFile.length()];
        aBufferedPrivateKey.read(aPrivateKeyBytes);
        
        KeySpec aKeySpec = new PKCS8EncodedKeySpec(aPrivateKeyBytes);
        RSAPrivateKey aPrivateKey = (RSAPrivateKey)KeyFactory.getInstance("RSA").generatePrivate(aKeySpec);
        
        ISerializer aSerializer = new RsaDigitalSignatureSerializer(aPublicCertificate, aPrivateKey);
        
        // Create receiver that receives MyRequestMsg and
        // responses MyResponseMsg
        IDuplexTypedMessagesFactory aReceiverFactory = new DuplexTypedMessagesFactory(aSerializer);
        myReceiver = aReceiverFactory.createDuplexTypedMessageReceiver(Integer.class, MyRequestMsg.class);
        
        // Subscribe to handle incoming messages.
        myReceiver.messageReceived().subscribe(myOnMessageReceived);
        
        // Create input channel listening to TCP.
        // Note: Silverlight can communicate only on ports: 4502 - 4532
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:4502/");

        // Attach the input channel to the receiver and start the listening.
        myReceiver.attachDuplexInputChannel(anInputChannel);
        
        System.out.println("Calculator service is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        
        // Detach the duplex input channel and stop the listening.
        // Note: it releases the thread listening to messages.
        myReceiver.detachDuplexInputChannel();
        
        // Stop the TCP policy server.
        aPolicyServer.stopPolicyServer();
        
        System.out.println("Calculator service stopped.");
    }
    
    private static void onMessageReceived(Object sender, TypedRequestReceivedEventArgs<MyRequestMsg> e)
    {
        // Calculate incoming numbers.
        int aResult = e.getRequestMessage().Number1 + e.getRequestMessage().Number2;
        
        System.out.println(e.getRequestMessage().Number1 + " + " + e.getRequestMessage().Number2 + " = " + aResult);
        
        // Response back the result.
        try
        {
            myReceiver.sendResponseMessage(e.getResponseReceiverId(), aResult);
        }
        catch (Exception err)
        {
            EneterTrace.error("Sending the response message failed.", err);
        }
    }
    

    // Handler used to subscribe for incoming messages.
    private static EventHandler<TypedRequestReceivedEventArgs<MyRequestMsg>> myOnMessageReceived
            = new EventHandler<TypedRequestReceivedEventArgs<MyRequestMsg>>()
    {
        @Override
        public void onEvent(Object sender, TypedRequestReceivedEventArgs<MyRequestMsg> e)
        {
            onMessageReceived(sender, e);
        }
    };
}

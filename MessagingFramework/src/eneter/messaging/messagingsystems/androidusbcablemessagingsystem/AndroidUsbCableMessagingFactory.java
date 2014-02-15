/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.androidusbcablemessagingsystem;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.connectionprotocols.EneterProtocolFormatter;
import eneter.messaging.messagingsystems.connectionprotocols.IProtocolFormatter;
import eneter.messaging.messagingsystems.messagingsystembase.*;

/**
 * Factory creating duplex output channels for the communication with Android device via the USB cable.
 * 
 *
 * When Android device is connected to the computer via the USB cable the process adb (Android Debug Bridge) is started
 * on the computer and adbd (Android Debug Bridge Daemon) is started on the Android device.
 * These processes then communicate via the USB cable.<br/>
 * <br/>
 * How this messaging works:
 * <ol>
 * <li>Your desktop application sends a message via the output channel created by AndroidUsbCableMessagingFactory</li>
 * <li>The output channel internally sends the message via TCP to the adb service.</li>
 * <li>adb service receives data and transfers it via USB cable to adbd.</li>
 * <li>adbd in the Android device receives data and forwards it via TCP to the desired port.</li>
 * <li>Android application listening on that port receives the message and processes it.</li>
 * </ol>
 * Notice there is a restrction for this type of communication:<br/>
 * The Android application must be a listener (service) and the computer application must be the client.<br/>
 * <br/>
 * The example shows a service on the Android side that will receive messages
 * via the USB cable.
 * <pre>
 * public class AndroidUsbCableServiceActivity extends Activity
 * {
 *     // Eneter communication.
 *     private IDuplexTypedMessageReceiver&lt;String, String&gt; myEchoReceiver;
 *     
 *     
 *     // Called when the activity is first created.
 *     {@literal @}Override
 *     public void onCreate(Bundle savedInstanceState)
 *     {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.main);
 *         
 *         // Start listening.
 *         startListening();
 *     }
 *     
 *     {@literal @}Override
 *     public void onDestroy()
 *     {
 *         stopListening();
 *         
 *         super.onDestroy();
 *     }
 *     
 *     private void startListening()
 *     {
 *         try
 *         {
 *             // Create message receiver.
 *             IDuplexTypedMessagesFactory aReceiverFactory = new DuplexTypedMessagesFactory();
 *             myEchoReceiver = aReceiverFactory.createDuplexTypedMessageReceiver(String.class, String.class);
 *             
 *             // Subscribe to receive messages.
 *             myEchoReceiver.messageReceived().subscribe(new EventHandler&lt;TypedRequestReceivedEventArgs&lt;String&gt;&gt;()
 *                 {
 *                     {@literal @}Override
 *                     public void onEvent(Object sender, TypedRequestReceivedEventArgs&lt;String&gt; e)
 *                     {
 *                         // Response back with the same message.
 *                         try
 *                         {
 *                             myEchoReceiver.sendResponseMessage(e.getResponseReceiverId(), e.getRequestMessage());
 *                         }
 *                         catch (Exception err)
 *                         {
 *                             EneterTrace.error("Sending echo response failed.", err);
 *                         }
 *                     }
 *                 });
 *             
 *             // Create TCP messaging.
 *             // Note: When adbd receives a message from the USB cable it will forward it
 *             //       to 127.0.0.1 (loopback) and desired port.
 *             IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 *             IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8090/");
 *             
 *             // Attach the input channel to the receiver and start listening.
 *             myEchoReceiver.attachDuplexInputChannel(anInputChannel);
 *         }
 *         catch (Exception err)
 *         {
 *             EneterTrace.error("OpenConnection failed.", err);
 *         }
 *     }
 *     
 *     private void stopListening()
 *     {
 *         // Detach input channel and stop listening.
 *         myEchoReceiver.detachDuplexInputChannel();
 *     }
 * }
 * </pre>
 * 
 * The example shows a client communicating with the Android service via the USB cable. 
 * <pre>
 * public class Program
 * {
 *     private static ISyncDuplexTypedMessageSender&lt;String, String&gt; mySender;
 * 
 *     public static void main(String[] args)
 *     {
 *         try
 *         {
 *             // Use messaging via Android USB cable.
 *             IMessagingSystemFactory aMessaging = new AndroidUsbCableMessagingFactory();
 *             
 *             // Specify that the android application will listen on the port 8090.
 *             // Note: adb (on PC) and adbd (on Android) will be configured to forward the
 *             //       the communication to the port 8090.
 *             IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("8090");
 *         
 *             // Create message sender.
 *             IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory();
 *             mySender = aSenderFactory.createSyncDuplexTypedMessageSender(String.class, String.class);
 *             
 *             // Attach the output channel using Android USB cable and
 *             // be able to send messages and receive responses.
 *             mySender.attachDuplexOutputChannel(anOutputChannel);
 *             
 *             // Send request and wait for response.
 *             String aResponse = mySender.sendRequestMessage("Hello");
 *             
 *             // Detach output channel and close the connection.
 *             mySender.detachDuplexOutputChannel();
 *             
 *             System.out.println("Android responded: " + aResponse);
 *         }
 *         catch (Exception err)
 *         {
 *             EneterTrace.error("Error detected.", err);
 *         }
 *     }
 * 
 * }
 * </pre>
 * 
 */
public class AndroidUsbCableMessagingFactory implements IMessagingSystemFactory
{
    /**
     * Constructs the Android messaging via the USB cable.
     * It expects the adb service listens on default port 5037.
     */
    public AndroidUsbCableMessagingFactory()
    {
        this(5037, new EneterProtocolFormatter());
    }
    
    /**
     * Constructs the Android messaging via the USB cable with specified parameters.
     * 
     * @param adbHostPort Port where adb service is listening to commands. Default value is 5037.
     * @param protocolFormatter Low level formatting used for encoding messages between channels.
     *  EneterProtocolFormatter() can be used by default.
     */
    public AndroidUsbCableMessagingFactory(int adbHostPort, IProtocolFormatter<byte[]> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myAdbHostPort = adbHostPort;
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages via the USB cable to the duplex input channel on Android device.
     * 
     * @param channelId Port number where the Android application is listening.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new AndroidUsbDuplexOutputChannel(Integer.parseInt(channelId), null, myAdbHostPort, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates the duplex output channel sending messages via the USB cable to the duplex input channel on Android device.
     * 
     * @param channelId Port number where the Android application is listening.
     * @param responseReceiverId Identifies the response receiver of this duplex output channel.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new AndroidUsbDuplexOutputChannel(Integer.parseInt(channelId), responseReceiverId, myAdbHostPort, myProtocolFormatter);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Not supported. The known restriction is that Android cannot be client. Therefore, .NET or Java application
     * running on PC cannot be a service using the duplex input chanel for listening. :-(
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        throw new UnsupportedOperationException("Duplex input channel is not supported for Android USB cable messaging.");
    }

    
    private int myAdbHostPort;
    private IProtocolFormatter<byte[]> myProtocolFormatter;
}

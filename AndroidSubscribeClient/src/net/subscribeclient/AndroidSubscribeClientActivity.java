package net.subscribeclient;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;
import eneter.messaging.messagingsystems.composites.BufferedMonitoredMessagingFactory;
import eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit.MonitoredMessagingFactory;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.NoneSecurityClientFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.NoneSecurityServerFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.messaging.nodes.broker.*;
import eneter.net.system.EventHandler;
import android.app.Activity;
import android.os.*;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

public class AndroidSubscribeClientActivity extends Activity
{
    // Declaration of your message you want to
    // use for the notification.
    public static class MyNotifyMsg
    {
        public String TextMessage;
    }
    
    // Communication
    private IDuplexBrokerClient myBrokerClient;
    // Serializer used to deserialize notification messages.
    private XmlStringSerializer mySerializer = new XmlStringSerializer();
    
    // UI controls
    private Handler myRefresh = new Handler(); 
    private Button mySubscribeBtn;
    private Button myUnsubscribeBtn;
    private EditText myReceivedNotifyEditTxt;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Subscribe Button.
        mySubscribeBtn = (Button) findViewById(R.id.subscribeBtn);
        mySubscribeBtn.setOnClickListener(myOnSubscribeBtnClickHandler);
        
        // Unsubscribe Button.
        myUnsubscribeBtn = (Button) findViewById(R.id.unsubscribeBtn);
        myUnsubscribeBtn.setOnClickListener(myOnUnsubscribeBtnClickHandler);
        
        // Message notified from the .NET application.
        myReceivedNotifyEditTxt = (EditText) findViewById(R.id.receivedNotificationEditText);
        
        try
        {
            openConnection();
        }
        catch (Exception err)
        {
            EneterTrace.error("Opening the connection failed.", err);
        }
    }
    
    private void openConnection() throws Exception
    {
        // Tracing
        //EneterTrace.setDetailLevel(EDetailLevel.Debug);
        
        // Create broker client.
        IDuplexBrokerFactory aBrokerFactory = new DuplexBrokerFactory();
        myBrokerClient = aBrokerFactory.createBrokerClient();
        
        // Subscribe to receive notification messages pushed
        // from the service.
        myBrokerClient.brokerMessageReceived().subscribe(myNotifyMessageHandler);
        
        // Create buffered monitored messaging using TCP.
        // The client will send the ping message every 20 minutes and will expect
        // the response for that ping within 1 minute.
        // If the response is not received, the client considers the connection
        // broken and tries to close it. Then it will try to reconnect.
        // Sent messages are stored in the buffer when the connection is broken.
        // When the connection is reopen messages from the buffer are sent.
        IMessagingSystemFactory aMessaging = new BufferedMonitoredMessagingFactory(
            new TcpMessagingSystemFactory(), // underlying messaging system
            new GZipSerializer(),// new XmlStringSerializer(), // use default serializer
            60000 * 60,     // max offline time is 1 hour
            60000 * 20,     // client sends ping once per 20 minutes
            60000);         // expect response for the ping within 1 minute
        
        //IMessagingSystemFactory aMessaging = new MonitoredMessagingFactory(
        //        new TcpMessagingSystemFactory(), // underlying messaging system
        //        new GZipSerializer(),// new XmlStringSerializer(), // use default serializer
        //        60000 * 20,     // client sends ping once per 20 minutes
        //        60000);         // expect response for the ping within 1 minute
        
        
        IDuplexOutputChannel anOutputChannel =
                //aMessaging.createDuplexOutputChannel("tcp://10.0.2.2:7091/");
                aMessaging.createDuplexOutputChannel("tcp://192.168.1.102:7091/");
        
        // Attach the output channel and be able to subscribe, unsubscribe
        // and receive notify messages pushed from the service.
        myBrokerClient.attachDuplexOutputChannel(anOutputChannel);
    }
    
    private void onSubscribeBtnClick(View v)
    {
        // Subscribe for the the notification message.
        try
        {
            myBrokerClient.subscribe("MyNotifyMessageId");
        }
        catch (Exception err)
        {
            EneterTrace.error("Subscribing for messages failed.", err);
        }
    }
    
    private void onUnsubscribeBtnClick(View v)
    {
        myReceivedNotifyEditTxt.setText("");
        
        // Unsubscribe from the the notification message.
        try
        {
            myBrokerClient.unsubscribe("MyNotifyMessageId");
        }
        catch (Exception err)
        {
            EneterTrace.error("Unsubscribing from messages failed.", err);
        }
    }
    
    private void onNotifyMessageReceived(Object sender, BrokerMessageReceivedEventArgs e)
    {
        try
        {
            // Check the message id.
            // Note: you can be subscribed for more message types.
            if (e.getMessageTypeId().equals("MyNotifyMessageId"))
            {
                // Deserialize the incoming message.
                final MyNotifyMsg aMsg = mySerializer.deserialize(e.getMessage(), MyNotifyMsg.class);
            
                // The notify message from the service was received.
                // Let's display it - displaying must be marshaled to the UI thread.
                myRefresh.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        myReceivedNotifyEditTxt.setText(aMsg.TextMessage);
                    }
                });
            }
        }
        catch (Exception err)
        {
            EneterTrace.error("Deserialization of the message failed.", err);
        }
    }
    
    private EventHandler<BrokerMessageReceivedEventArgs> myNotifyMessageHandler
        = new EventHandler<BrokerMessageReceivedEventArgs>()
        {
            @Override
            public void onEvent(Object sender, BrokerMessageReceivedEventArgs e)
            {
                onNotifyMessageReceived(sender, e);
            }
        };
    
    private OnClickListener myOnSubscribeBtnClickHandler = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            onSubscribeBtnClick(v);
        }
    };
    
    private OnClickListener myOnUnsubscribeBtnClickHandler = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            onUnsubscribeBtnClick(v);
        }
    };
}
package eneter.tests.echoclient;

import java.net.URI;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IClientSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.SslClientFactory;
import eneter.messaging.messagingsystems.websocketmessagingsystem.*;
import eneter.net.system.EventHandler;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

public class AndroidWebSocketEchoClientActivity extends Activity
{
    // UI Controls.
    private Handler myRefresh = new Handler();
    private EditText myEchoServiceAddressEditText;
    private EditText myEchoRequestMessageEdittext;
    private TextView myEchoResponseMessageTextView;
    private Button mySendBtn;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        myEchoServiceAddressEditText = (EditText) findViewById(R.id.echoServiceAddressEditText);
        myEchoRequestMessageEdittext = (EditText) findViewById(R.id.echoRequestMessageEditText);
        mySendBtn = (Button) findViewById(R.id.sendEchoRequestBtn);
        mySendBtn.setOnClickListener(myOnSendBtnClick);
        myEchoResponseMessageTextView = (TextView) findViewById(R.id.echoResponseMessageTextView);
    }
    
    private void onSendBtnClick(View v)
    {
        // Get the echo service address.
        String anAddress = myEchoServiceAddressEditText.getText().toString();
        
        final WebSocketClient aWebSocketClient;
        try
        {
            // Get URI of the echo service.
            URI anEchoServiceUri = new URI(anAddress);

            // Determine if the echo service is SSL.
            if (anEchoServiceUri.getScheme().toLowerCase().equals("wss"))
            {
                aWebSocketClient = new WebSocketClient(anEchoServiceUri, new SslClientFactory());
            }
            else
            {
                aWebSocketClient = new WebSocketClient(anEchoServiceUri);
            }
            
            // Open websocket connection.
            aWebSocketClient.openConnection();
            
            // Subscribe to receive response from the echo service.
            aWebSocketClient.messageReceived().subscribe(new EventHandler<WebSocketMessage>()
                {
                    @Override
                    public void onEvent(Object sender, WebSocketMessage e)
                    {
                        try
                        {
                            // Get the response from the echo service.
                            final String aResponseMessage = e.getWholeTextMessage();
                            
                            // Display the response in the correct UI!
                            myRefresh.post(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        myEchoResponseMessageTextView.setText(aResponseMessage);
                                    }
                                });
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error("Receiving the echo response failed.", err);
                        }
                        
                        // Close the websocket connection.
                        aWebSocketClient.closeConnection();
                    }
                });
            
            // Send the message.
            String aMessageText = myEchoRequestMessageEdittext.getText().toString();
            aWebSocketClient.sendMessage(aMessageText);
        }
        catch (Exception err)
        {
            EneterTrace.error("sending of the message failed.", err);
        }
    }
    
    
    private OnClickListener myOnSendBtnClick = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            onSendBtnClick(v);
        }
    };
}
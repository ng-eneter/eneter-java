package net.eneter.messaging.messagingsystems.tcpmessagingsystem;

import org.perfectjpattern.core.extras.validate.Validate;

import android.net.Uri;
import android.util.Log;

import com.Eneter.Messaging.MessagingSystems.MessagingSystemBase.IOutputChannel;

public class TcpOutputChannel
implements IOutputChannel
{	
	public TcpOutputChannel(String channelId)
	{
		try
		{
			Validate.notNull(channelId, "TcpOutputChannel failed during construction because the specified channel id is null or empty string.");
		}
		catch(Exception err)
		{
			Log.e("TcpOutputChannel", err.getMessage(), err);
		}
		
		Uri.Builder myUriBuilder = new Uri.Builder();
        try
        {
        	myUriBuilder.authority(channelId);
            // just check if the address is valid
        	myUriBuilder.build();
        }
        catch (Exception err)
        {
        	Log.e("TcpOutputChannel", err.getMessage(), err);
        }

        myChannelId = channelId;
	}

	@Override
	public String GetChannelId()
	{
		return myChannelId;
	}

	@Override
	public void SendMessage(Object message)
	{		
		try {
			099.
			//
			100.
			// Create a connection to the server socket on the server application
			101.
			//
			102.
			InetAddress host = InetAddress.getLocalHost();
			103.
			Socket socket = new Socket(host.getHostName(), 7777);
			104.
			 
			105.
			//
			106.
			// Send a message to the client application
			107.
			//
			108.
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			109.
			oos.writeObject("Hello There...");
			110.
			 
			111.
			//
			112.
			// Read and display the response message sent by server application
			113.
			//
			114.
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
			115.
			String message = (String) ois.readObject();
			116.
			System.out.println("Message: " + message);
			117.
			 
			118.
			ois.close();
			119.
			oos.close();
			120.
			} catch (UnknownHostException e) {
			121.
			e.printStackTrace();
			122.
			} catch (IOException e) {
			123.
			e.printStackTrace();
			124.
			} catch (ClassNotFoundException e) {
			125.
			e.printStackTrace();
			126.
			}
			127.

	}
	
	private Uri.Builder myUriBuilder;
	private String myChannelId;
}

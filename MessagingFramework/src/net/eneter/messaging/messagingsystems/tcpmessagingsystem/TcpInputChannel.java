package net.eneter.messaging.messagingsystems.tcpmessagingsystem;

import org.perfectjpattern.core.api.behavioral.observer.ISubject;
import com.Eneter.Messaging.MessagingSystems.MessagingSystemBase.ChannelMessageEventArgs;
import com.Eneter.Messaging.MessagingSystems.MessagingSystemBase.IInputChannel;

public class TcpInputChannel extends TcpInputChannelBase
implements IInputChannel		
{
	public TcpInputChannel(String channelId)
	{

	}

	@Override
	public String GetChannelId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean GetIsListening() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ISubject<ChannelMessageEventArgs> MessageReceived() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void StartListening() {
		// TODO Auto-generated method stub

	}

	@Override
	public void StopListening() {
		// TODO Auto-generated method stub

	}

}

package net.eneter.messaging.messagingsystems.tcpmessagingsystem;

import com.Eneter.Messaging.MessagingSystems.MessagingSystemBase.*;

/**
 * The factory class implements the messaging system delivering messages via Tcp. <br/>
 * It creates output and input channels using Tcp. <br/>
 * The channel id must be a valid Uri address representing an Ip address (e.g. 127.0.0.1:8091).
 * 
 * @author vachix
 *
 */
public class TcpMessagingSystemFactory 
implements  IMessagingSystemFactory
{
	/**
	 * Creates the output channel sending messages to specified input channel using Tcp.
	 * 
	 * @param channelId Identifies the receiving input channel. The channel id must be a valid Ip address of the receiver. e.g. 127.0.0.1:8090
	 * @return output channel
	 */
    public IOutputChannel CreateOutputChannel(String channelId)
    {
        return new TcpOutputChannel(channelId);
    }

    /**
     * Creates the input channel listening to messages on the specified channel id, using Tcp.
     * 
     * @param channelId Identifies this input channel. The channel id must be a valid Ip address (e.g. 127.0.0.1:8090) the input channel will listen to.
     * @return input channel
     */
    public IInputChannel CreateInputChannel(String channelId)
    {
        return new TcpInputChannel(channelId);
    }
}
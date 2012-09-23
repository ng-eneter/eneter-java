/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * The event data available when the input channel receives a message.
 * 
 */
public final class ChannelMessageEventArgs 
{
	/**
	 * Constructs the event data from the input parameters.
	 * 
	 * @param channelId Identifies the receiver of messages. (e.g. tcp://127.0.0.1:8090/)
	 * @param message Message
	 * @param senderAddress Address where the sender of the message is located. (e.g. IP address of the client)<br/>
     * Can be empty string if not applicable in used messaging.
	 */
    public ChannelMessageEventArgs(String channelId, Object message, String senderAddress)
    {
        myChannelId = channelId;
        myMessage = message;
        mySenderAddress = senderAddress;
    }

    /**
     * Returns the channel id identifying the receiver of messages. (e.g. tcp://127.0.0.1:8090/).
     */
    public String getChannelId()
    {
    	return myChannelId;
    }

    /**
     * Returns the message.
     */
    public Object getMessage()
    {
    	return myMessage;
    }
    
    /**
     * Returns the address where the sender of the message is located. (e.g. IP address of the client).
     * It can be empty string if not applicable for used messaging.
     * @return
     */
    public String getSenderAddress()
    {
        return mySenderAddress;
    }
    
    /**
     * The channel identifier.
     */
    private String myChannelId;
    
    /**
     * The message
     */
    private Object myMessage;
    
    /**
     * The sender address. e.g. IP address of the client.
     * If not applicable for the messaging then empty string.
     */
    private String mySenderAddress;
}

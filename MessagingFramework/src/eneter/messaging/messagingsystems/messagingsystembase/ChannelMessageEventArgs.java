/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */
package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * The event data available when the input channel receives a message.
 * 
 * @author Ondrej Uzovic & Martin Valach
 */
public final class ChannelMessageEventArgs 
{
	/**
	 * Constructs the event data from the input parameters.
	 * 
	 * @param channelId Channel identifier
	 * @param message Message
	 */
    public ChannelMessageEventArgs(String channelId, Object message)
    {
        myChannelId = channelId;
        myMessage = message;
    }

    /**
     * Returns the channel identifier.
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
     * The channel identifier.
     */
    private String myChannelId;
    
    /**
     * The message
     */
    private Object myMessage;
}

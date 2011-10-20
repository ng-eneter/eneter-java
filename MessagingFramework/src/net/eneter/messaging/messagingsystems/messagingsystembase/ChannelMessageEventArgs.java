/**
 * 
 */
package net.eneter.messaging.messagingsystems.messagingsystembase;

import org.perfectjpattern.core.api.behavioral.observer.data.*;

/**
 * The event data available when the input channel receives a message.
 * 
 * @author vachix
 *
 */
public final
class ChannelMessageEventArgs 
implements IEventData
{
    //------------------------------------------------------------------------
    // public
    //------------------------------------------------------------------------
	/**
	 * Constructs the event data from the input parameters.
	 * @param channelId The channel identifier
	 * @param message A message
	 */
    public ChannelMessageEventArgs(String channelId, Object message)
    {
        myChannelId = channelId;
        myMessage = message;
    }

    /**
     * Returns the channel identifier.
     */
    public String GetChannelId()
    {
    	return myChannelId;
    }

    /**
     * Returns the message.
     * @return Object
     */
    public Object GetMessage()
    {
    	return myMessage;
    }
    
    //------------------------------------------------------------------------
    // members
    //------------------------------------------------------------------------
    /**
     * The channel identifier.
     */
    private String myChannelId;
    
    /**
     * The message
     */
    private Object myMessage;
}

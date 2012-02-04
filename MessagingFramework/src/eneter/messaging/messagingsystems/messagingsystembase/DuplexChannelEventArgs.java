/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * Event argument containing channel id and response receiver id.
 * 
 */
public class DuplexChannelEventArgs
{
    /**
     * Constructs the event argument.
     * @param channelId channel id
     * @param responseReceiverId response receiver id
     */
    public DuplexChannelEventArgs(String channelId, String responseReceiverId)
    {
        myChannelId = channelId;
        myResponseReceiverId = responseReceiverId;
    }
    
    /**
     * Returns channel id.
     */
    public String getChannelId()
    {
        return myChannelId;
    }
    
    /**
     * Returns response receiver id.
     */
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }
    
    private String myChannelId;
    private String myResponseReceiverId;
}

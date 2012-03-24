/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * The event data available when the duplex input channel receives a message.
 * 
 */
public final class DuplexChannelMessageEventArgs extends DuplexChannelEventArgs
{
    /**
     * Constructs the event.
     * @param channelId channel id
     * @param message received message
     * @param responseReceiverId response receiver id
     */
    public DuplexChannelMessageEventArgs(String channelId, Object message, String responseReceiverId)
    {
        super(channelId, responseReceiverId);
        
        myMessage = message;
    }

    /**
     * Returns received message.
     */
    public Object getMessage()
    {
        return myMessage;
    }
    
    private Object myMessage;
}

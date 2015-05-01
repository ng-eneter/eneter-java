/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * Event argument used by output and input channel to indicate a message was received.
 * 
 */
public final class DuplexChannelMessageEventArgs extends DuplexChannelEventArgs
{
    /**
     * Constructs the event.
     * @param channelId Identifies the receiver of request messages. (e.g. tcp://127.0.0.1:8090/)
     * @param message received message
     * @param responseReceiverId Unique logical id identifying the receiver of response messages.
     * @param senderAddress Address where the sender of the message is located. (e.g. IP address of the client)<br/>
     * Can be empty string if not applicable in used messaging.
     */
    public DuplexChannelMessageEventArgs(String channelId, Object message,
            String responseReceiverId, String senderAddress)
    {
        super(channelId, responseReceiverId, senderAddress);
        
        myMessage = message;
    }

    /**
     * Returns message.
     */
    public Object getMessage()
    {
        return myMessage;
    }
    
    private Object myMessage;
}

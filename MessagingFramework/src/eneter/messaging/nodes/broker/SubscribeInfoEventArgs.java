/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

/**
 * Event argument used when the broker subscribed or unsubscribed a client.
 *
 */
public class SubscribeInfoEventArgs
{
    /**
     * Constructs the event.
     * @param subscriberResponseReceiverId response reciver id of subscriber
     * @param messageTypes message ids which are subscribed or unsubscribed
     */
    public SubscribeInfoEventArgs(String subscriberResponseReceiverId, String[] messageTypes)
    {
        mySubscriberResponseReceiverId = subscriberResponseReceiverId;
        myMessageTypeIds = messageTypes;
    }

    /**
     * Returns subscriber response receiver id.
     * @return response receiver id
     */
    public String GetSubscriberResponseReceiverId()
    {
        return mySubscriberResponseReceiverId;
    }
    
    /**
     * Returns message ids which the client subscribed or unsubscribed.
     * @return message type ids
     */
    public String[] GetMessageTypeIds()
    {
        return myMessageTypeIds;
    }
    
    private String mySubscriberResponseReceiverId;
    private String[] myMessageTypeIds;
}

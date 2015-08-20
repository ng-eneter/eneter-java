/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

/**
 * Event argument used when the broker published a message.
 *
 */
public class PublishInfoEventArgs
{
    /**
     * Constructs the event.
     * @param publishedResponseReceiverId response receiver id of the publisher.
     * @param publishedMessageTypeId message type which was published.
     * @param publishedMessage message which was published.
     * @param publishedToSubscribers number of subscribers to which the message was published.
     */
    public PublishInfoEventArgs(String publishedResponseReceiverId, String publishedMessageTypeId, Object publishedMessage, int publishedToSubscribers)
    {
        myPublisherResponseReceiverId = publishedResponseReceiverId;
        myPublishedMessageTypeId = publishedMessageTypeId;
        myPublishedMessage = publishedMessage;
        myNumberOfSubscribers = publishedToSubscribers;
    }
    
    /**
     * Returns response receiver id of the publisher.
     * @return response receiver id
     */
    public String GetPublisherResponseReceiverId()
    {
        return myPublisherResponseReceiverId;
    }
    
    /**
     * Returns id of message type which was published.
     * @return message type id
     */
    public String GetPublishedMessageTypeId()
    {
        return myPublishedMessageTypeId;
    }
    
    /**
     * Returns published message.
     * @return message data
     */
    public Object GetPublishedMessage()
    {
        return myPublishedMessage;
    }
    
    /**
     * Returns number of subscribers to which the message was published.
     * @return number of subscribers
     */
    public int GetNumberOfSubscribers()
    {
        return myNumberOfSubscribers;
    }
    
    private String myPublisherResponseReceiverId;
    private String myPublishedMessageTypeId;
    private Object myPublishedMessage;
    private int myNumberOfSubscribers;
}

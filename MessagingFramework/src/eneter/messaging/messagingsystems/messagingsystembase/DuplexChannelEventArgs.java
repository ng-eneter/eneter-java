/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.messagingsystembase;

/**
 * Event argument containing parameters of the communication.
 * 
 */
public class DuplexChannelEventArgs
{
    /**
     * Constructs the event argument.
     * @param channelId Identifies the receiver of request messages. (e.g. tcp://127.0.0.1:8090/)
     * @param responseReceiverId Unique logical id identifying the receiver of response messages.
     * @param senderAddress Address where the sender of the request message is located. (e.g. IP address of the client)<br/>
     * Can be empty string if not applicable in used messaging.
     */
    public DuplexChannelEventArgs(String channelId, String responseReceiverId, String senderAddress)
    {
        myChannelId = channelId;
        myResponseReceiverId = responseReceiverId;
        mySenderAddress = senderAddress;
    }
    
    /**
     * Returns the channel id identifying the receiver of request messages. (e.g. tcp://127.0.0.1:8090/).
     */
    public String getChannelId()
    {
        return myChannelId;
    }
    
    /**
     * Returns the unique logical id identifying the receiver of response messages.
     */
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
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
    
    private String myChannelId;
    private String myResponseReceiverId;
    private String mySenderAddress;
}

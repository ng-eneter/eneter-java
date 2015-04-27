/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.io.Serializable;

/**
 * Internal message used for the communication between multi-typed message sender and receiver.
 *
 */
public class MultiTypedMessage implements Serializable
{
    /**
     * Name of the message type (without namespace).
     */
    public String TypeName;
    
    /**
     * Serialized message.
     */
    public Object MessageData;

    private static final long serialVersionUID = 4149527018926629618L;
}

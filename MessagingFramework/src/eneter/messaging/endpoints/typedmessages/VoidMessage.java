/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.io.Serializable;


/**
 * Represents an empty data type 'void'.
 * Can be used if no type is expected as a message.
 * <pre>
 * The following example shows how to use VoidMessage to declare a message sender
 * sending string messages and receiving "nothing".
 * <br/>
 * {@code
 * ...
 * IDuplexTypedMessageSender<VoidMessage, String> myMessageSender;
 * ...
 * }
 * </pre>
 */
public class VoidMessage implements Serializable
{
    private static final long serialVersionUID = -1919328266369375421L;
}

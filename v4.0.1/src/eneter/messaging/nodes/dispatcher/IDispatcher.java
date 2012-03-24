/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

import eneter.messaging.infrastructure.attachable.*;

/**
 * Declares the dispatcher.
 * 
 * The dispatcher has attached more input channels and more output channels.<br/>
 * When it receives some message via the input channel, it forwards the message to all output channels.<br/>
 * This is the one-way dispatcher. It means it can forward messages but cannot route back response messages.
 *
 */
public interface IDispatcher extends IAttachableMultipleOutputChannels,
                                     IAttachableMultipleInputChannels
{

}

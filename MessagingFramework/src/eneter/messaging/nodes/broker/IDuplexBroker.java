/**
 * Project: Eneter.Messaging.Framework for Java
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.broker;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;

/**
 * The interface declares the broker.
 * The broker receives messages and forwards them to subscribed clients.
 * @author Ondrej Uzovic
 *
 */
public interface IDuplexBroker extends IAttachableDuplexInputChannel
{

}

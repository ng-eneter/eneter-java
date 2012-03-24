/**
 * Project: Eneter.Messaging.Framework
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
 *
 */
public interface IDuplexBroker extends IAttachableDuplexInputChannel
{

}

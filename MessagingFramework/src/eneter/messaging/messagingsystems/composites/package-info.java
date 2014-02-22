/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */



/**
 * Extensions for messaging systems.
 * 
 * E.g. it is possible to extend the communication by additional features like: connection monitoring, buffering, authentication or communication via the message bus.
 *
 * The composite implements IMessagingSystemFactory so it looks like any other messaging but it provides
 * some additional behavior which is then applied on the underlying messaging.
 * Multiple composite messaging systems can be applied in a "chain". 
 * E.g. if you want to have TCP communication with monitored connection and authentication you can
 * compose it like in the following example. 
 * 
 * 
 * <pre>
 * // Create TCP messaging system.
 * IMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();
 * <br/>
 * // Create monitored messaging which takes TCP as underlying messaging.
 * IMessagingSystemFactory aMonitoredMessaging = new MonitoredMessagingFactory(aTcpMessaging);
 * <br/>
 * // Create messaging with authenticated connection.
 * // It takes monitored messaging as the underlying messaging.
 * IMessagingSystemFactory aMessaging = new AuthenticatedMessagingFactory(aMonitoredMessaging, ...);
 * <br/>
 * // Then creating channels.
 * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8095/");
 * IDuplexInputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8095/");
 * </pre>
 * 
 */
package eneter.messaging.messagingsystems.composites;
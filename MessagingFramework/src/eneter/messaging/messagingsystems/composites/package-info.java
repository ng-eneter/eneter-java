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
 * The composites are extensions which can be composed on top of each other in order to add additional features
 * into the communication.
 * E.g. connection monitoring, connection recovery, authentication or communication via the message bus.<br/>
 * <br/>
 * The following example shows how to add the connection monitoring and the authentication into the communication via TCP.
 * <pre>
 * {@code
 * // Create TCP messaging system.
 * IMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();
 * 
 * // Create monitored messaging which takes TCP as underlying messaging.
 * IMessagingSystemFactory aMonitoredMessaging = new MonitoredMessagingFactory(anUnderlyingMessaging);
 * 
 * // Create messaging with authenticated connection.
 * // It takes monitored messaging as the underlying messaging.
 * IMessagingSystemFactory aMessaging = new AuthenticatedMessagingFactory(aMonitoredMessaging, ...);
 * 
 * // Creating channels.
 * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8095/");
 * IDuplexInputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8095/");
 * }
 * </pre>
 * 
 */
package eneter.messaging.messagingsystems.composites;
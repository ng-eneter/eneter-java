/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Functionality extending the default behavior of messaging systems.
 * 
 * E.g.: Buffering of sent messages or network connection monitoring.<br/>
 * The extensions are realized by so called 'composites'. The composite is a messaging system derived from
 * IMessagingSystemFactory that implements the extending functionality but for the communication
 * it uses other (underlying) messaging system. It means, e.g. if you wish buffered messaging for the TCP based communication,
 * you can create the buffered messaging system using the TCP as the underlying messaging system.
 * <pre>
 * Creating the buffered messaging using the TCP as the underlying messaging system.<br/>
 * {@code
 * // Create TCP messaging system.
 * IMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();
 *
 * // Create the buffered messaging using TCP as the underlying messaging.
 * IMessagingSystemFactory aBufferedMessaging = new BufferedMessagingFactory(anUnderlyingMessaging);
 * }
 * </pre>
 * Therefore, messaging systems can be composed into layers providing the desired functionality. It is also possible
 * to use a composite messaging system as the underlying messaging.
 * <pre>
 * Creating the TCP based messaging system constantly checking the network connection and providing the buffer
 * for sent messages in case of the disconnection.<br/>
 * {@code
 * // Create TCP messaging system.
 * IMessagingSystemFactory aTcpMessaging = new TcpMessagingSystemFactory();
 *
 * // Create the composite providing the network connection monitor.
 * IMessagingSystemFactory aMonitoredMessaging = new MonitoredMessagingFactory(aTcpMessaging);
 *
 * // Create the composite providing the buffer used for the sent messages in case of the disconnection.
 * IMessagingSystemFactory aBufferedMonitoredMessaging = new BufferedMessagingFactory(aMonitoredMessaging);
 *
 *
 * ...
 *
 * // Create the duplex output channel, that monitores the network connection and buffers sent messages if disconnected.
 * IDuplexOutputChannel aDuplexOutputChannel = aBufferedMonitoredMessaging.CreateDuplexOutputChannel("tcp://127.0.0.1:6080/");
 *
 * }
 * </pre>
 * To simplify the implementation, there are already pre-implemented typically composed messaging systems.
 * E.g.: <see cref="ReliableMonitoredMessagingFactory"/>. It is the messaging system that is composed from the following
 * layers:
 * <br/>
 * <ol>
 * <li>Monitored Messaging --> constantly checks the connection.</li>
 * <li>Messaging System    --> a real messaging system transferring messages, e.g. TCP messaging system.</li>
 * </ol>
 * <br/>
 * Notice, that the communicating applications (or components) must use the same composite messaging system to be able to communicate.
 * E.g. if the server application uses MonitoredMessagingFactory then also the client must use MonitoredMessagingFactory.
 * Otherwise, they will not understand each other.
 */
package eneter.messaging.messagingsystems.composites;
/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


/**
 * Functionality to send and receive text messages.
 * 
 * The following example shows how to send a string message via TCP.<br/>
 * <pre>
 * {@code
 * ...
 * // Create string message sender.
 * IDuplexStringMessagesFactory aSenderFactory = new DuplexStringMessagesFactory();
 * IDuplexStringMessageSender aSender = aSenderFactory.createDuplexStringMessageSender();
 *
 * // Subscribe to receive response messages.
 * aSender.responseReceived().subscribe(...);
 *
 * // Create TCP messaging.
 * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8033/");
 *
 * // Attach output channel and be able to send messages and receive responses.
 * aSender.attachDuplexOutputChannel(anOutputChannel);
 * ...
 * }
 * </pre>
 * <br/>
 * The following example shows how to receive a string message via TCP.<br/>
 * <pre>
 * {@code
 * ...
 * // Create string message receiver.
 * IDuplexStringMessagesFactory aReceiverFactory = new DuplexStringMessagesFactory();
 * IDuplexStringMessageReceiver aReceiver = aReceiverFactory.createDuplexStringMessageReceiver();
 *
 * // Subscribe to receive messages.
 * aReceiver.requestReceived().subscribe(...);
 *
 * // Create TCP messaging.
 * IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
 * IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8033/");
 *
 * // Attach input channel and start listening to messages.
 * aReceiver.attachDuplexInputChannel(anInputChannel);
 * }
 * </pre>
 */
package eneter.messaging.endpoints.stringmessages;
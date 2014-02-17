/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Buffering of sent messages if the connection is not available.
 * 
 *
 * The buffered messaging is intended to temporarily store sent messages until the network connection is established.<br/>
 * Typical scenarios are:
 * <br/><br/>
 * <b>Short disconnections</b><br/>
 * In case of unstable the network the connection can broken. Buffered messaging will try to reconnect the broken connection
 * and meanwhile it will store sent messages in the buffer. Then when the connection is repaired it will send messages from
 * the buffer.
 * <br/><br/>
 * <b>Independent startup order</b><br/>
 * It can be tricky to start communicating application in a defined order. Buffered messaging allows to start
 * applications in undefined order. If messages are sent to an application which is not started yet they will be stored
 * in the buffer until the application is started.
 * 
 * <pre>
 * Simple client buffering messages in case of a disconnection.

 * // Create TCP messaging.
 * IMessagingSystemFactory anUnderlyingMessaging = new TcpMessagingSystemFactory();
 * 
 * // Create buffered messaging that internally uses TCP.
 * IMessagingSystemFactory aMessaging = new BufferedMessagingSystemFactory(anUnderlyingMessaging);
 * 
 * // Create the duplex output channel.
 * IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8045/");
 * 
 * // Create message sender to send simple string messages.
 * IDuplexStringMessagesFactory aSenderFactory = new DuplexStringMessagesFactory();
 * IDuplexStringMessageSender aSender = aSenderFactory.CreateDuplexStringMessageSender();
 * 
 * // Subscribe to receive responses.
 * aSender.responseReceived().subscribe(myOnResponseReceived);
 * 
 * // Attach output channel an be able to send messages and receive responses.
 * aSender.attachDuplexOutputChannel(anOutputChannel);
 * 
 * ...
 * 
 * // Send a message.
 * // If the connection is broken the message will be stored in the buffer.
 * // Note: The buffered messaging will try to reconnect automatically.
 * aSender.SendMessage("Hello.");

 * </pre>
 *
 */
package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;
/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Extends the messaging system to work temporarily offline while the connection is not available.
 * 
 * The buffered messaging is intended to overcome relatively short time intervals when the connection is not available.
 * It means, the buffered messaging is able to hide the connection is not available and work offline while
 * trying to reconnect.<br/>
 * If the connection is not available, the buffered messaging stores sent messages (and sent response messages)
 * in the buffer and sends them when the connection is established.<br/>
 * Buffered messaging also checks if the between duplex output channel and duplex input channel is active.
 * If the connection is not used (messages do not flow between client and service) the buffered messaging
 * waits the specified maxOfflineTime and then disconnects the client.
 *  
 * Typical scenarios for buffered messaging:
 * <br/><br/>
 * <b>Short disconnections</b><br/>
 * The network connection is unstable and can be anytime interrupted. In case of the disconnection, sent messages are stored
 * in the buffer while the connection tries to be reopen. If the connection is established again,
 * the messages are sent from the buffer.<br/>
 * <br/>
 * <b>Independent startup order</b><br/>
 * The communicating applications starts in undefined order and initiates the communication. 
 * The buffered messaging stores messages in the buffer while receiving application is started and ready to receive
 * messages.<br/> 
 * <br/>
 * <b>Note:</b><br/>
 * The buffered messaging does not require, that both communicating parts create channels with buffered messaging factory.
 * It means, e.g. the duplex output channel created with buffered messaging with underlying TCP, can send messages
 * directly to the duplex input channel created with just TCP messaging factory.
 */
package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;
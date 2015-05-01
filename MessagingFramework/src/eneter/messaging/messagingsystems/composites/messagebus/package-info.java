/**
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/


/**
 * Extension for communication via the message bus.
 * 
 * The message bus is the component that can be used to expose multiple services from one place.
 * It means when a service wants to expose its functionality it connects the message bus and registers its service id.
 * Then when a client wants to use the service it connects the message bus and asks for the service using the service id.
 * Message bus is then responsible to establish the connection between the client and the service.<br/>
 * This extension hides the communication is running via the message bus. For communicating parts it looks as if they
 * communicate directly.
 * 
 *
 */
package eneter.messaging.messagingsystems.composites.messagebus;
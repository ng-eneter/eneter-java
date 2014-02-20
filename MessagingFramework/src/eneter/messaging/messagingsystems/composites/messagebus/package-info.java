/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/


/**
 * Communication via the message bus.
 * 
 * The message bus is the component that can be used to expose multiple services from one place.
 * When a service wants to expose its functionality via the message bus it connects the message bus and registers there.
 * Then when a client wants to use the service it connects the message bus and asks for the service.
 * If the requested service is registered the communication between the client and the service is mediated via the message bus.<br/>
 * 
 *
 */
package eneter.messaging.messagingsystems.composites.messagebus;
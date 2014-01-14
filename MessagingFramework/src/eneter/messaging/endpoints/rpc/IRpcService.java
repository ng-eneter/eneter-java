/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.*;

/**
 * Declares service receiving requests via remote procedure calls.
 *
 * @param <TServiceInterface> Service interface. 
 * The provided type must be a non-generic interface which can declare methods and events.
 * Methods arguments and return value cannot be generic.
 * (Events are declared as methods without arguments that returns Event<...>.)  
 */
public interface IRpcService<TServiceInterface> extends IAttachableDuplexInputChannel
{
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
}

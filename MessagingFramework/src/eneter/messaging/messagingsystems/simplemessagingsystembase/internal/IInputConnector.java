/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.net.system.IFunction1;

public interface IInputConnector
{
    void startListening(IFunction1<Boolean, MessageContext> messageHandler) throws Exception;
    void stopListening();
    boolean isListening();

    ISender createResponseSender(String responseReceiverAddress) throws Exception;
}

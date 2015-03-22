/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.net.system.IMethod1;

public interface IInputConnector
{
    void startListening(IMethod1<MessageContext> messageHandler) throws Exception;
    void stopListening();
    boolean isListening();

    void sendResponseMessage(String outputConnectorAddress, Object message) throws Exception;

    void closeConnection(String outputConnectorAddress) throws Exception;
}

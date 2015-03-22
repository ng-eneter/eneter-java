/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.net.system.IMethod1;

public interface IOutputConnector
{
    void openConnection(IMethod1<MessageContext> responseMessageHandler) throws Exception;
    void closeConnection();
    boolean isConnected();
    void sendRequestMessage(Object message) throws Exception;
}

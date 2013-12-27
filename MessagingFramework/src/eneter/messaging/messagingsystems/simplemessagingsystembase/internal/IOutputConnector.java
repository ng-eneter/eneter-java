/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import eneter.net.system.IFunction1;

public interface IOutputConnector extends ISender
{
    void openConnection(IFunction1<Boolean, MessageContext> responseMessageHandler) throws Exception;
    void closeConnection();
    boolean isConnected();
}

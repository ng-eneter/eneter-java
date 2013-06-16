/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

public interface IClientConnectorFactory
{
    IClientConnector createClientConnector(String serviceConnectorAddress, String clientConnectorAddress) throws Exception;
}

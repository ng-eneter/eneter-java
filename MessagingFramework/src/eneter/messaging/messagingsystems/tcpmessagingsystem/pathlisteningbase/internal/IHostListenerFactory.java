/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.internal;

import java.net.InetSocketAddress;

import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;

public interface IHostListenerFactory
{
    Class<?> getListenerType();
    
    HostListenerBase CreateHostListener(InetSocketAddress address, IServerSecurityFactory securityFactory);
}

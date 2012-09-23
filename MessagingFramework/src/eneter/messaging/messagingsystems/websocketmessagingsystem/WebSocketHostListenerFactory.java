/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.InetSocketAddress;

import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.internal.*;


class WebSocketHostListenerFactory implements IHostListenerFactory
{

    @Override
    public Class<?> getListenerType()
    {
        return WebSocketHostListener.class;
    }

    @Override
    public HostListenerBase CreateHostListener(InetSocketAddress address, IServerSecurityFactory securityFactory)
    {
        WebSocketHostListener aPathListener = new WebSocketHostListener(address, securityFactory);
        return aPathListener;
    }
    
}

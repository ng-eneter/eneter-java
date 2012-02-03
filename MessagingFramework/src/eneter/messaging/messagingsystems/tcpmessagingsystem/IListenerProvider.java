/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.Socket;

import eneter.net.system.IMethod1;

public interface IListenerProvider
{
    public void startListening(IMethod1<Socket> connectionHandler) throws Exception;
    
    public void stopListening();
    
    public boolean isListening() throws Exception;
}

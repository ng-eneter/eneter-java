package eneter.messaging.messagingsystems.tcpmessagingsystem;

import java.net.Socket;

import eneter.net.system.IMethod1;

public interface ITcpListenerProvider
{
    public void startListening(IMethod1<Socket> connectionHandler) throws Exception;
    
    public void stopListening();
    
    public boolean isListening() throws Exception;
}

package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.net.Socket;

import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.net.system.IMethod1;

public class HttpListenerProvider implements ITcpListenerProvider
{

    @Override
    public void startListening(IMethod1<Socket> connectionHandler)
            throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stopListening()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isListening()
    {
        // TODO Auto-generated method stub
        return false;
    }

    
    
}

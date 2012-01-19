package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.net.*;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.*;
import eneter.net.system.IMethod1;

class HttpListenerProvider implements ITcpListenerProvider
{
    public HttpListenerProvider(URI uri, IServerSecurityFactory serverSecurityFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUri = uri;
            myServerSecurityFactory = serverSecurityFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void startListening(IMethod1<Socket> connectionHandler)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                HttpListenerController.startListening(myUri, connectionHandler, myServerSecurityFactory);
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
            throw err;
        }
        catch (Error err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
            throw err;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void stopListening()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                HttpListenerController.stopListening(myUri);
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
        }
        catch (Error err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.StartListeningFailure, err);
            throw err;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isListening() throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myListeningManipulatorLock)
            {
                return HttpListenerController.isListening(myUri);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    private Object myListeningManipulatorLock = new Object();
    private URI myUri;
    private IServerSecurityFactory myServerSecurityFactory;
   
    private String TracedObject()
    {
        return "HttpListenerProvider ";
    }
}

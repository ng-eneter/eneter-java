package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.net.URI;

import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.NoneSecurityServerFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.internal.PathListenerProviderBase;
import eneter.net.system.IMethod1;


class HttpListener
{
    private static class HttpListenerImpl extends PathListenerProviderBase
    {
        public HttpListenerImpl(URI webSocketUri)
        {
            super(new HttpHostListenerFactory(), webSocketUri, new NoneSecurityServerFactory());
        }
        
        public HttpListenerImpl(URI webSocketUri, IServerSecurityFactory securityFactory)
        {
            super(new HttpHostListenerFactory(), webSocketUri, securityFactory);
        }

        @Override
        protected String TracedObject()
        {
            return "HttpListener ";
        }
    }
    
    
    
    public HttpListener(URI webSocketUri)
    {
        myListenerImpl = new HttpListenerImpl(webSocketUri);
    }
    
    public HttpListener(URI webSocketUri, IServerSecurityFactory securityFactory)
    {
        myListenerImpl = new HttpListenerImpl(webSocketUri, securityFactory);
    }
    
    
    public void startListening(final IMethod1<HttpRequestContext> connectionHandler)
            throws Exception
    {
        myListenerImpl.startListening(new IMethod1<Object>()
            {
                @Override
                public void invoke(Object t) throws Exception
                {
                    if (t instanceof HttpRequestContext)
                    {
                        connectionHandler.invoke((HttpRequestContext)t);
                    }
                }
            });
    }
    
    
    /**
     * Stops listening and closes all open connections with clients.
     */
    public void stopListening()
    {
        myListenerImpl.stopListening();
    }
    
    /**
     * Returns true if the service is listening.
     * @return true if listening.
     * @throws Exception
     */
    public boolean isListening()
    {
        return myListenerImpl.isListening();
    }
    
    /**
     * Returns address of the service.
     */
    public URI getAddress()
    {
        return myListenerImpl.getAddress();
    }
    
    
    private HttpListenerImpl myListenerImpl;
}

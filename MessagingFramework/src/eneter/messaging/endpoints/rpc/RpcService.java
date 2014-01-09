package eneter.messaging.endpoints.rpc;

import java.util.HashSet;

import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;


class RpcService<TServiceInterface> extends AttachableDuplexInputChannelBase
                                    implements IRpcService<TServiceInterface>
{
    // Maintains events and subscribed clients.
    private class EventContext
    {
        public EventContext(TServiceInterface service, EventInfo eventInfo, Delegate handler)
        {
            myService = service;
            EventInfo = eventInfo;
            myHandler = handler;
            SubscribedClients = new HashSet<string>();
        }

        public void Subscribe()
        {
            EventInfo.AddEventHandler(myService, myHandler);
        }

        public void Unsubscribe()
        {
            EventInfo.RemoveEventHandler(myService, myHandler);
        }

        public Class<?> getEventInfo()
        {
            return myEventInfo;
        }
        
        public HashSet<String> getSubscribedClients()
        {
            return mySubscribedClients;
        }
        
        private Class<?> myEventInfo;
        private HashSet<String> mySubscribedClients;

        private TServiceInterface myService;
        private EventHandler myHandler;
    }
    

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void onRequestMessageReceived(Object sender,
            DuplexChannelMessageEventArgs e)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }

}

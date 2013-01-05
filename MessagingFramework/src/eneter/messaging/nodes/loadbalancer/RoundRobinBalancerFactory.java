package eneter.messaging.nodes.loadbalancer;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;

public class RoundRobinBalancerFactory implements ILoadBalancerFactory
{
    public RoundRobinBalancerFactory(IMessagingSystemFactory duplexOutputChannelsFactory)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myDuplexOutputChannelsFactory = duplexOutputChannelsFactory;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public ILoadBalancer createLoadBalancer()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new RoundRobinBalancer(myDuplexOutputChannelsFactory);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private IMessagingSystemFactory myDuplexOutputChannelsFactory;
}

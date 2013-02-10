/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.loadbalancer;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;

/**
 * Factory that creates the load balancer based on Round-Robin algorithm.
 *
 *
 * The Round-Robin balancer distributes the incoming requests equally to all maintained receivers.
 * It means, the balancer maintains which receiver was used the last time. Then, when a new request comes,
 * the balancer picks the next receiver in the list up. If it is at the end, then it starts from the beginning.
 *
 */
public class RoundRobinBalancerFactory implements ILoadBalancerFactory
{
    /**
     * Constructs the factory.
     *
     * @param duplexOutputChannelsFactory
     * messaging system used to create duplex output channels that will be used for the communication with
     * services from the farm.
     * 
     */
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
    
    /**
     * Creates the load balancer using the Round-Robin algorithm.
     */
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

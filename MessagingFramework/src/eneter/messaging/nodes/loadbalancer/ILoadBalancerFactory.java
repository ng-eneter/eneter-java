/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.loadbalancer;

/**
 * Creates the load balancer.
 *
 *
 * The load balancer distributes the workload across a farm of receivers that can run on different machines (or threads).
 *
 */
public interface ILoadBalancerFactory
{
    /**
     * Creates the load balancer.
     * @return load balancer
     */
    ILoadBalancer createLoadBalancer();
}

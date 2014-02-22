/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

/**
 * Distributing the workload across a farm of receivers.
 *
 * The load balancer maintains a list of receivers processing a certain request.
 * When the balancer receives the request, it chooses which receiver shall process it,
 * so that all receivers are loaded optimally.
 *
 */
package eneter.messaging.nodes.loadbalancer;
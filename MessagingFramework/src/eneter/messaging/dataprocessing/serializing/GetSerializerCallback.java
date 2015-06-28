/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.serializing;

public interface GetSerializerCallback
{
    ISerializer invoke(String responseReceiverId);
}

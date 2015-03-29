/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.typedmessages;

import java.io.Serializable;

public class MultiTypedMessage implements Serializable
{
    public String TypeName;
    
    public Object MessageData;

    private static final long serialVersionUID = 4149527018926629618L;
}

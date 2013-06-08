/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2013 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.simplemessagingsystembase.internal;

import java.io.OutputStream;

import eneter.net.system.IMethod1;

public interface ISender
{
    boolean isStreamWritter();
    void sendMessage(Object message) throws Exception;
    void sendMessage(IMethod1<OutputStream> toStreamWritter) throws Exception;
}

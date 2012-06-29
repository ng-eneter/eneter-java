/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.websocketmessagingsystem;

class WebSocketFrame
{
    public WebSocketFrame(EFrameType frameType, boolean maskFlag, byte[] message, boolean isFinal)
    {
        FrameType = frameType;
        MaskFlag = maskFlag;
        Message = message;
        IsFinal = isFinal;
    }
    
    
    public final EFrameType FrameType;
    public final boolean MaskFlag;
    public final byte[] Message;
    public final boolean IsFinal;
}

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

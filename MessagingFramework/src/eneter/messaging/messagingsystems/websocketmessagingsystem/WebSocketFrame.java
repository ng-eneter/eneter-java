package eneter.messaging.messagingsystems.websocketmessagingsystem;

class WebSocketFrame
{
    public WebSocketFrame(EFrameType frameType, boolean maskFlag, byte[] message, boolean isFinal)
    {
        myFrameType = frameType;
        myMaskFlag = maskFlag;
        myMessage = message;
        myIsFinal = isFinal;
    }
    
    
    public EFrameType getFrameType()
    {
        return myFrameType;
    }
    
    public boolean getMaskFlag()
    {
        return myMaskFlag;
    }
    
    public byte[] getMessage()
    {
        return myMessage;
    }
    
    public boolean isFinal()
    {
        return myIsFinal;
    }
    
    private EFrameType myFrameType;
    private boolean myMaskFlag;
    private byte[] myMessage;
    private boolean myIsFinal;
}

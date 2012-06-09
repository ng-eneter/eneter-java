package eneter.messaging.messagingsystems.websocketmessagingsystem;

enum EFrameType
{
    Continuation(0x00),
    Text(0x1),
    Binary(0x2),
    Close(0x8),
    Ping(0x9),
    Pong(0xA);
    
    private EFrameType(int value)
    {
        myValue = value;
    }
    
    public int getValue()
    {
        return myValue;
    }
    
    public static EFrameType getEnum(int value)
    {
        for (EFrameType e : EFrameType.values())
        {
            if (value == e.getValue())
            {
                return e;
            }
        }
        
        throw new IllegalStateException("Failed to cast int to enum. Unknown int value.");
    }
     
    private int myValue;
}

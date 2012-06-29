/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */


package eneter.messaging.messagingsystems.websocketmessagingsystem;

/**
 * Defines data frames as specified by the websocket protocol.
 *
 */
enum EFrameType
{
    /**
     * Frame contains message data that was not sent in one 'Text' or 'Binary'.
     * Message that is split into multiple frames.
     */
    Continuation(0x00),
    
    /**
     * Frame contains UTF8 text message data.
     */
    Text(0x1),
    
    /**
     * Frame contains binary data.
     */
    Binary(0x2),
    
    /**
     * Control frame indicating the connection goes down.
     */
    Close(0x8),
    
    /**
     * Control frame pinging the end-point (client or server). The pong response is expected.
     */
    Ping(0x9),
    
    /**
     * Control frame as a response for the ping.
     */
    Pong(0xA);
    
    private EFrameType(int value)
    {
        myValue = value;
    }
    
    public int getValue()
    {
        return myValue;
    }
    
    /**
     * Casts integer value to the enum.
     * @param value
     * @return
     */
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

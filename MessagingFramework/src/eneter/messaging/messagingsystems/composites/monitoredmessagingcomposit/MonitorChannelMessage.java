package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.io.Serializable;

public class MonitorChannelMessage implements Serializable
{
    public MonitorChannelMessage()
    {
    }
    
    public MonitorChannelMessage(MonitorChannelMessageType messageType, Object messageContent)
    {
        myMessageType = messageType;
        myMessageContent = messageContent;
    }
    
    public MonitorChannelMessageType myMessageType;
    public Object myMessageContent;

    private static final long serialVersionUID = -1411932226587371691L;
}

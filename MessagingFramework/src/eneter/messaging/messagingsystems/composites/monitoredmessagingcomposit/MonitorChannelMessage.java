package eneter.messaging.messagingsystems.composites.monitoredmessagingcomposit;

import java.io.Serializable;

public class MonitorChannelMessage implements Serializable
{
    public MonitorChannelMessage()
    {
    }
    
    public MonitorChannelMessage(MonitorChannelMessageType messageType, Object messageContent)
    {
        MessageType = messageType;
        MessageContent = messageContent;
    }
    
    public MonitorChannelMessageType MessageType;
    public Object MessageContent;

    private static final long serialVersionUID = -1411932226587371691L;
}

package eneter.messaging.messagingsystems.connectionprotocols;

public class ProtocolMessage
{
    public ProtocolMessage()
    {
    }

    public ProtocolMessage(EProtocolMessageType messageType, String responseReceiverId, Object message)
    {
        MessageType = messageType;
        ResponseReceiverId = responseReceiverId;
        Message = message;
    }

    public EProtocolMessageType MessageType;
    public String ResponseReceiverId;
    public Object Message;
}

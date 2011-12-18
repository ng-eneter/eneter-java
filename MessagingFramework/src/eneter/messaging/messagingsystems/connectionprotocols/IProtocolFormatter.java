package eneter.messaging.messagingsystems.connectionprotocols;

import java.io.InputStream;

public interface IProtocolFormatter<T>
{
    T encodeOpenConnectionMessage(String responseReceiverId) throws Exception;

    T encodeCloseConnectionMessage(String responseReceiverId) throws Exception;

    T encodeMessage(String responseReceiverId, Object message) throws Exception;

    T encodePollRequest(String responseReceiverId) throws Exception;

    ProtocolMessage decodeMessage(InputStream readStream);

    ProtocolMessage decodeMessage(Object readMessage);
}

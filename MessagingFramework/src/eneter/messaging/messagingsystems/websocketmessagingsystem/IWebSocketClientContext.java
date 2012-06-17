package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.URI;

import eneter.net.system.Event;

public interface IWebSocketClientContext
{
    Event<Object> connectionClosed();
    
    Event<Object> pongReceived();
    
    boolean isConnected();
    
    URI getAddress();
    
    void sendMessage(Object data);
    
    void sendMessage(Object data, boolean isFinal);
    
    WebSocketMessage receiveMessage();
    
    void sendPing();
    
    void sendPong();
    
    void closeConnection();
}

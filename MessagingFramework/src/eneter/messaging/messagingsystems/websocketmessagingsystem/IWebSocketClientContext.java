package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.net.URI;

import eneter.net.system.Event;

public interface IWebSocketClientContext
{
    Event<Object> connectionClosed();
    
    Event<Object> pongReceived();
    
    boolean isConnected();
    
    URI getAddress();
    
    void sendMessage(Object data) throws Exception;
    
    void sendMessage(Object data, boolean isFinal) throws Exception;
    
    WebSocketMessage receiveMessage() throws Exception;
    
    void sendPing() throws Exception;
    
    void sendPong() throws Exception;
    
    void closeConnection();
}

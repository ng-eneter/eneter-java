package eneter.messaging.messagingsystems.composites.messagebus;

/**
 * Internal commands for interaction with the message bus.
 *
 */
public enum EMessageBusRequest
{
    /**
     * Used by service when registering to the message bus.
     * MessageBusMessage Id parameter is service id which shall be registered.
     */
    RegisterService(10),
    
    /**
     * Used by client when connecting the service via the message bus.
     * MessageBusMessage Id parameter is service id which shall be connected.
     */
    ConnectClient(20),
    
    /**
     * Used by service when it wants to disconnect a particular client.
     * MessageBusMessage Id parameter is client id which shall be disconnected.
     */
    DisconnectClient(30),
    
    /**
     * Used by service when it confirms the client was connected.
     * MessageBusMessage Id parameter is client id which was connected to the service.
     */
    ConfirmClient(40),
    
    /**
     * Used by client when sending a message to the service.
     * MessageBusMessage Id parameter is client id which sent the message to the service.
     */
    SendRequestMessage(50),
    
    /**
     * Used by service when sending message to the client.
     * MessageBusMessage Id parameter is client id which shall receive the message.
     */
    SendResponseMessage(60);
          
    /**
     * Converts enum to the integer value.
     * @return
     */
    public int geValue()
    {
        return myValue;
    }
    
    /**
     * Converts integer value to the enum.
     * @param i value
     * @return enum
     */
    public static EMessageBusRequest fromInt(int i)
    {
        switch (i)
        {
            case 10: return RegisterService;
            case 20: return ConnectClient;
            case 30: return DisconnectClient;
            case 40: return ConfirmClient;
            case 50: return SendRequestMessage;
            case 60: return SendResponseMessage;
        }
        return null;
    }
    
    private EMessageBusRequest(int value)
    {
        myValue = value;
    }

    private final int myValue;
}

/**
 * 
 */
package eneter.messaging.messagingsystems.simplemessagingsystembase;

import eneter.net.system.IMethod1;

/**
 *
 */
public interface IMessagingProvider
{
    void SendMessage(String receiverId, Object message);
    
    void RegisterMessageHandler(String receiverId, IMethod1<Object> messageHandler);

    void UnregisterMessageHandler(String receiverId);
}

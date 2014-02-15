/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

import eneter.messaging.infrastructure.attachable.IAttachableDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.net.system.*;

/**
 * Declares service which can receive requests via RPC (Remote Procedure Call).
 * 
 * RpcService acts as a stub which provides the communication functionality for an instance implementing the given service interface.
 *
 * @param <TServiceInterface> Service interface. 
 *
 * The provided service type must be an interface fulfilling following criteria:
 * <ul>
 * <li>Interface is not generic.</li>
 * <li>Methods, arguments and return values are not generic.</li>
 * <li>Methods are not overloaded. It means there are no two methods with the same name.</li>
 * <li>Events like in C# language are supported too - see the example below.</li>
 * </ul>
 * 
 * The following example shows how to declare a service interface.
 * <pre>
 * {@code
 * // Declaring the service.
 * public interface IHello
 * {
 *     // Declares event which notifies String.
 *     // Note: This event would not be compatible in cross Java/C# communication.
 *     Event&lt;String&gt; someEventHappend();
 *     
 *     // Declares event which notifies EventArgs.
 *     // Note: event arguments derived from EventArgs can be used in cross Java/C# communication.
 *     Event&lt;EventArgs&gt; otherEventHappened();
 *     
 *     // Declares method taking arguments and returning a value.
 *     int calculate(int a, int b);
 *     
 *     // Declares method taking arguments and returning void.
 *     // Note: MyArgumentType must be serializable. It means the serializer used for the communication
 *     //       must be able to serialize it.
 *     void performSomething(MyArgumentType param); 
 * }
 * }
 * </pre>
 * 
 * The following example shows how to declare interface that can be used for Java/C# communication.
 * <pre>
 * {@code
 * // C# interface 
 * public interface IMyInterface
 * {
 *    // Event without arguments.
 *    event EventHandler SomethingHappened;
 *    
 *    // Event with arguments.
 *    event EventHandler<MyEventArgs> SomethingElseHappened;
 *    
 *    // Simple method.
 *    void DoSomething();
 *    
 *    // Method with arguments.
 *    int Calculate(int a, int b);
 * }
 * .
 * // Java equivalent
 * // Note: methods names must be exactly same as in C#.
 * public interface IMyInterface
 * {
 *    // Event without arguments.
 *    Event<EventArgs> SomethingHappened();
 *    
 *    // Event with arguments.
 *    Event<MyArgs> SomethingElseHappened();
 *    
 *    // Simple method.
 *    void DoSomething();
 *    
 *    // Method with arguments.
 *    int Calculate(int a, int b);
 * }
 * }
 * </pre>
 */
public interface IRpcService<TServiceInterface> extends IAttachableDuplexInputChannel
{
    /**
     * Event raised when a client connected the service.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverConnected();
    
    /**
     * Event raised when a client got disconnected from the service.
     * @return
     */
    Event<ResponseReceiverEventArgs> responseReceiverDisconnected();
}

package eneter.messaging.endpoints.rpc;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;



/**
 * Creates RPC clients and services of the given type.
 * 
 * The provided service type must be an interface with following parameters:
 * <ul>
 * <li>Interface is not generic.</li>
 * <li>Methods, arguments and return values are not generic.</li>
 * <li>Methods are not overloaded. It means there are no two methods with the same name but different input parameters.</li>
 * <li>Events like in C# language are supported too - se the example below.</li>
 * </ul>
 * 
 * Example showing how to declare interfaces for C# - Java communication.
 * <pre>
 * {@code
 * // C# interface 
 * public interface IMyInterface
 * {
 *    // Event without arguments.
 *    event EventHandler SomethingHappened;
 *    
 *    // Event with arguments.
 *    event EventHandler<MyArgs> SomethingElseHappened;
 *    
 *    // Simple method.
 *    void Hello();
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
 *    void Hello();
 *    
 *    // Method with arguments.
 *    int Calculate(int a, int b);
 * }
 * }
 * </pre>
 *
 */
public class RpcFactory implements IRpcFactory
{
    public RpcFactory()
    {
        this(new XmlStringSerializer());
    }
    
    public RpcFactory(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;

            // Default timeout is set to infinite by default.
            myRpcTimeout = 0;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <TServiceInterface> IRpcClient<TServiceInterface> createClient(Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new RpcClient<TServiceInterface>(mySerializer, 1000, clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <TServiceInterface> IRpcService<TServiceInterface> createService(TServiceInterface service, Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            return new RpcService<TServiceInterface>(service, mySerializer ,clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    public ISerializer getSerializer()
    {
        return mySerializer;
    }
    
    public RpcFactory setSerializer(ISerializer serializer)
    {
        mySerializer = serializer;
        return this;
    }
    
    public int getRpcTimeout()
    {
        return myRpcTimeout;
    }
    
    public RpcFactory setRpcTimeout(int rpcTimeout)
    {
        myRpcTimeout = rpcTimeout;
        return this;
    }
    
    private ISerializer mySerializer;
    private int myRpcTimeout;
}

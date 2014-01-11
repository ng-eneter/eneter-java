package eneter.messaging.endpoints.rpc;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;

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

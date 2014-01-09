package eneter.messaging.endpoints.rpc;

import eneter.messaging.dataprocessing.serializing.*;

public class RpcFactory implements IRpcFactory
{

    @Override
    public <TServiceInterface> IRpcClient<TServiceInterface> createClient(Class<TServiceInterface> clazz)
    {
        ISerializer aSerializer = new XmlStringSerializer();
        return new RpcClient<TServiceInterface>(aSerializer, 1000, clazz);
    }

    @Override
    public <TServiceInterface> IRpcService<TServiceInterface> createService(
            TServiceInterface service)
    {
        // TODO Auto-generated method stub
        return null;
    }

}

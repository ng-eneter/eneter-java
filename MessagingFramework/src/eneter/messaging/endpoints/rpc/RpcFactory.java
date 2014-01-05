package eneter.messaging.endpoints.rpc;

public class RpcFactory implements IRpcFactory
{

    @Override
    public <TServiceInterface> IRpcClient<TServiceInterface> createClient(Class<TServiceInterface> clazz)
    {
        return new RpcClient<TServiceInterface>(null, 1000, clazz);
    }

    @Override
    public <TServiceInterface> IRpcService<TServiceInterface> createService(
            TServiceInterface service)
    {
        // TODO Auto-generated method stub
        return null;
    }

}

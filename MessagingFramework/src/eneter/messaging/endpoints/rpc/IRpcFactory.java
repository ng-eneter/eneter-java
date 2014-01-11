package eneter.messaging.endpoints.rpc;

public interface IRpcFactory
{
    <TServiceInterface> IRpcClient<TServiceInterface> createClient(Class<TServiceInterface> clazz);
    
    <TServiceInterface> IRpcService<TServiceInterface> createService(TServiceInterface service, Class<TServiceInterface> clazz);
 
}

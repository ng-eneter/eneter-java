package eneter.messaging.endpoints.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

class ProxyProvider
{
    public static <TServiceInterface> TServiceInterface createInstance(InvocationHandler invocationHandler, Class<TServiceInterface> clazz)
    {
        ClassLoader aClassLoader = clazz.getClassLoader();
        Class<?>[] anImplementedInterfaces = { clazz };
        
        // Create dynamic proxy for the given interface.
        @SuppressWarnings("unchecked")
        TServiceInterface aProxyInstance = (TServiceInterface) Proxy.newProxyInstance(aClassLoader, anImplementedInterfaces, invocationHandler);
        
        return aProxyInstance;
    }

}

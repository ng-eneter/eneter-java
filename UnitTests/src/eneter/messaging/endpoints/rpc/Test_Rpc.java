package eneter.messaging.endpoints.rpc;

import org.junit.*;

import eneter.net.system.*;


public class Test_Rpc
{
    public static interface IFooService
    {
        Event<EventArgs> nonGenericEvent();
        Event<String> genericEvent();
        
        int Calculate();
    }
    
    @Test
    public void Test1()
    {
        RpcFactory aFactory = new RpcFactory();
        
        IRpcClient<IFooService> aClient = aFactory.createClient(IFooService.class);
    }
}

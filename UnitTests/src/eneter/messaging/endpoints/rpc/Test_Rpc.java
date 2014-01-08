package eneter.messaging.endpoints.rpc;

import java.util.ArrayList;

import org.junit.*;

import eneter.net.system.*;


public class Test_Rpc
{
    public static interface IFooService
    {
        Event<EventArgs> nonGenericEvent();
        Event<String> genericEvent();
        
        int Calculate(int a, int b);
        
        void DoIt();
    }
    
    @Test
    public void Test1() throws Exception
    {
        RpcFactory aFactory = new RpcFactory();
        
        IRpcClient<IFooService> aClient = aFactory.createClient(IFooService.class);
        
        aClient.callRemoteMethod("Calculate", new Object[] {1, 2});
    }
}

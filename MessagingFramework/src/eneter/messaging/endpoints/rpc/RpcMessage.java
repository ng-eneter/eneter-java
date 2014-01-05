package eneter.messaging.endpoints.rpc;

import java.io.Serializable;

public class RpcMessage implements Serializable
{
    public int Id;

    public int Flag;

    public String OperationName;

    public Object[] SerializedData;

    public String Error;

    private static final long serialVersionUID = 8365985506571587359L;
}

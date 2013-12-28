package eneter.messaging.messagingsystems.messagingsystembase;

public class ConnectionTokenEventArgs
{
    public ConnectionTokenEventArgs(String responseReceiverId, String senderAddress)
    {
        myResponseReceiverId = responseReceiverId;
        mySenderAddress = senderAddress;
        
        // Allow connection by default.
        myIsConnectionAllowed = true;
    }
    
    public String GetResponseReceiverId()
    {
        return myResponseReceiverId;
    }

    public String GetSenderAddress()
    {
        return mySenderAddress;
    }
    
    public void SetConnectionAllowed(boolean isConnectionAllowed)
    {
        myIsConnectionAllowed = isConnectionAllowed;
    }
    
    public boolean isConnectionAllowed()
    {
        return myIsConnectionAllowed;
    }
    
    private String myResponseReceiverId;
    private String mySenderAddress;
    private boolean myIsConnectionAllowed;
}

package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.IMethod2;

class ResponseReceiverContext
{
    public ResponseReceiverContext(String responseReceiverId, IDuplexInputChannel duplexInputChannel, IMethod2<String, Boolean> lastActivityUpdater)
    {
        myResponseReceiverId = responseReceiverId;
        mySender = new ResponseMessageSender(responseReceiverId, duplexInputChannel, lastActivityUpdater);
        myLastActivityTime = System.currentTimeMillis();
    }
    
    public void updateLastActivityTime()
    {
        myLastActivityTime = System.currentTimeMillis();
    }

    public void sendResponseMessage(Object message)
    {
        mySender.sendResponseMessage(message);
    }
    
    public void stopSendingOfResponseMessages() throws Exception
    {
        mySender.stopSending();
    }
    
    public String getResponseReceiverId()
    {
        return myResponseReceiverId;
    }
    
    public long getLastActivityTime()
    {
        return myLastActivityTime;
    }
    
    private String myResponseReceiverId;
    public long myLastActivityTime;

    private ResponseMessageSender mySender;
}

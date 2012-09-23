/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.internal.IMethod3;

class ResponseReceiverContext
{
    public ResponseReceiverContext(String responseReceiverId, String clientAddress, IDuplexInputChannel duplexInputChannel, IMethod3<String, String, Boolean> lastActivityUpdater)
    {
        myResponseReceiverId = responseReceiverId;
        myClientAddress = clientAddress;
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
    
    public String getClientAddress()
    {
        return myClientAddress;
    }
    
    public long getLastActivityTime()
    {
        return myLastActivityTime;
    }
    
    private String myResponseReceiverId;
    private String myClientAddress;
    private long myLastActivityTime;

    private ResponseMessageSender mySender;
}

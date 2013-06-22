/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.bufferedmessagingcomposit;

import eneter.messaging.messagingsystems.messagingsystembase.*;

class ResponseReceiverContext
{
    public ResponseReceiverContext(String responseReceiverId, String clientAddress, IDuplexInputChannel duplexInputChannel)
    {
        myResponseReceiverId = responseReceiverId;
        myClientAddress = clientAddress;
        mySender = new ResponseMessageSender(responseReceiverId, duplexInputChannel);
        
        setConnectionState(false);
    }
    
    public void setConnectionState(boolean isConnected)
    {
        synchronized (myResponseReceiverManipulatorLock)
        {
            myLastConnectionChangeTime = System.currentTimeMillis();
            myIsResponseReceiverConnected = isConnected;
        }
    }
    
    public boolean isResponseReceiverConnected()
    {
        synchronized (myResponseReceiverManipulatorLock)
        {
            return myIsResponseReceiverConnected;
        }
    }
    
    public void sendResponseMessage(Object message)
    {
        mySender.sendResponseMessage(message);
    }
    
    public void stopSendingOfResponseMessages()
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
    
    public void setClientAddress(String clientAddress)
    {
        myClientAddress = clientAddress;
    }
    
    public long getLastConnectionChangeTime()
    {
        return myLastConnectionChangeTime;
    }
    
    
    
    private String myResponseReceiverId;
    private String myClientAddress;
    private long myLastConnectionChangeTime;

    private ResponseMessageSender mySender;
    
    private boolean myIsResponseReceiverConnected;
    private Object myResponseReceiverManipulatorLock = new Object();
}

package eneter.messaging.endpoints.typedmessages;

import java.util.ArrayList;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.AutoResetEvent;

class SyncTypedMessageSender<TResponse, TRequest> implements ISyncTypedMessageSender<TResponse, TRequest>
{

    @Override
    public void attachDuplexOutputChannel(
            IDuplexOutputChannel duplexOutputChannel) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void detachDuplexOutputChannel()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isDuplexOutputChannelAttached()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IDuplexOutputChannel getAttachedDuplexOutputChannel()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TResponse SendRequestMessage(TRequest message) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // During sending and receiving only one caller is allowed.
            synchronized (myRequestResponseLock)
            {
                final ArrayList<TypedResponseReceivedEventArgs<TResponse>> aReceivedResponse = new ArrayList<TypedResponseReceivedEventArgs<TResponse>>();
                
                EventHandler<TypedResponseReceivedEventArgs<TResponse>> aResponseHandler = new EventHandler<TypedResponseReceivedEventArgs<TResponse>>()
                {
                    @Override
                    public void onEvent(Object x, TypedResponseReceivedEventArgs<TResponse> y)
                    {
                        aReceivedResponse.add(y);
                        myResponseAvailableEvent.set();
                    }
                }; 
                        
                mySender.responseReceived().subscribe(aResponseHandler);

                try
                {
                    mySender.sendRequestMessage(message);

                    // Wait auntil the response is received or the waiting was interrupted or timeout.
                    // Note: use int instead of TimeSpan due to compatibility reasons. E.g. Compact Framework does not support TimeSpan in WaitOne().
                    if (!myResponseAvailableEvent.waitOne(myResponseReceiveTimeout))
                    {
                        String anErrorMessage = TracedObject() + "failed to receive the response with the timeout. " + myResponseReceiveTimeout;
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }

                    // If response data does not exist.
                    if (aReceivedResponse.size() == 0)
                    {
                        String anErrorMessage = TracedObject() + "failed to receive the response.";

                        IDuplexOutputChannel anAttachedOutputChannel = mySender.getAttachedDuplexOutputChannel();
                        if (anAttachedOutputChannel == null)
                        {
                            anErrorMessage += " The duplex outputchannel was detached.";
                        }
                        else if (!anAttachedOutputChannel.isConnected())
                        {
                            anErrorMessage += " The connection was closed.";
                        }
                        
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }

                    // If an error occured during receving the response then throw exception.
                    Exception aReceivingError = aReceivedResponse.get(0).getReceivingError();
                    if (aReceivingError != null)
                    {
                        String anErrorMessage = TracedObject() + "failed to receive the response.";
                        EneterTrace.error(anErrorMessage, aReceivingError);
                        throw new IllegalStateException(anErrorMessage, aReceivingError);
                    }

                    return aReceivedResponse.get(0).getResponseMessage();
                }
                finally
                {
                    mySender.responseReceived().unsubscribe(aResponseHandler);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onConnectionClosed(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // The connection was interrupted therefore we must unblock the waiting request.
            myResponseAvailableEvent.set();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private Object myAttachDetachLock = new Object();
    private Object myRequestResponseLock = new Object();

    private AutoResetEvent myResponseAvailableEvent = new AutoResetEvent(false);

    private int myResponseReceiveTimeout;
    private IDuplexTypedMessageSender<TResponse, TRequest> mySender;
    
    private Class<TRequest> myRequestMessageClazz;
    private Class<TResponse> myResponseMessageClazz;
    
    
    private String TracedObject()
    {
        String aResponseMessageTypeName = (myResponseMessageClazz != null) ? myResponseMessageClazz.getSimpleName() : "...";
        String aRequestMessageTypeName = (myRequestMessageClazz != null) ? myRequestMessageClazz.getSimpleName() : "...";
        String aDuplexOutputChannelId = (getAttachedDuplexOutputChannel() != null) ? getAttachedDuplexOutputChannel().getChannelId() : "";
        return "SyncTypedMessageSender<" + aResponseMessageTypeName + ", " + aRequestMessageTypeName + "> atached to the duplex output channel '" + aDuplexOutputChannelId + "' ";
    }
}

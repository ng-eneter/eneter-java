package eneter.messaging.endpoints.multitypedmessages;

import static org.junit.Assert.*;

import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;

public abstract class SyncMultiTypedMessageBaseTester
{
    @Test
    public void SyncRequestResponse_MultipleTypes() throws Exception
    {
        final IMultiTypedMessageReceiver aReceiver = MultiTypedMessagesFactory.createMultiTypedMessageReceiver();
        aReceiver.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                try
                {
                    aReceiver.sendResponseMessage(y.getResponseReceiverId(), y.getRequestMessage().toString(), String.class);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Failed to send response message.", err);
                }
            }
        }, int.class);
        
        aReceiver.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<String>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<String> y)
            {
                try
                {
                    aReceiver.sendResponseMessage(y.getResponseReceiverId(), y.getRequestMessage().length(), int.class);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Failed to send response message.", err);
                }
            }
        }, String.class);
        
        ISyncMultitypedMessageSender aSender = MultiTypedMessagesFactory.createSyncMultiTypedMessageSender();

        try
        {
            aReceiver.attachDuplexInputChannel(InputChannel);
            aSender.attachDuplexOutputChannel(OutputChannel);

            String aResult1 = aSender.sendRequestMessage(100, String.class, int.class);
            assertEquals("100", aResult1);

            int aResult2 = aSender.sendRequestMessage("Hello", int.class, String.class);
            assertEquals(5, aResult2);
        }
        finally
        {
            aSender.detachDuplexOutputChannel();
            aReceiver.detachDuplexInputChannel();
        }
    }

    protected IDuplexInputChannel InputChannel;
    protected IDuplexOutputChannel OutputChannel;
    protected IMultiTypedMessagesFactory MultiTypedMessagesFactory;
}

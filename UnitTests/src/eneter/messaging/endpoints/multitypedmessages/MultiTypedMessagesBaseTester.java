package eneter.messaging.endpoints.multitypedmessages;

import static org.junit.Assert.*;

import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.AutoResetEvent;


public abstract class MultiTypedMessagesBaseTester
{
    public static class CustomClass
    {
        public String Name;
        public int Count;
    }
    
    protected void Setup(IMessagingSystemFactory messagingSystemFactory, String channelId, ISerializer serializer) throws Exception
    {
        MessagingSystemFactory = messagingSystemFactory;

        DuplexOutputChannel = MessagingSystemFactory.createDuplexOutputChannel(channelId);
        DuplexInputChannel = MessagingSystemFactory.createDuplexInputChannel(channelId);

        IMultiTypedMessagesFactory aMessageFactory = new MultiTypedMessagesFactory(serializer);
        Requester = aMessageFactory.createMultiTypedMessageSender();
        Responser = aMessageFactory.createMultiTypedMessageReceiver();
    }
    
    @Test
    public void SendReceive_Message() throws Exception
    {
        // The test can be performed from more threads therefore we must synchronize.
        final AutoResetEvent aMessageReceivedEvent = new AutoResetEvent(false);

        final int[] aReceivedMessage1 = { 0 };
        Responser.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                aReceivedMessage1[0] = y.getRequestMessage();
                
                // Send the response
                try
                {
                    Responser.sendResponseMessage(y.getResponseReceiverId(), "hello", String.class);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Failed to send response.", err);
                    aMessageReceivedEvent.set();
                }
            }
            
        }, int.class);
        
        final CustomClass[] aReceivedMessage2 = { null };
        Responser.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<CustomClass>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<CustomClass> y)
            {
                aReceivedMessage2[0] = y.getRequestMessage();

                // Send the response
                CustomClass aResponse = new CustomClass();
                aResponse.Name = "Car";
                aResponse.Count = 100;

                try
                {
                    Responser.sendResponseMessage(y.getResponseReceiverId(), aResponse, CustomClass.class);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Failed to send response.", err);
                    aMessageReceivedEvent.set();
                }
            }
    
        }, CustomClass.class);
        

        Responser.attachDuplexInputChannel(DuplexInputChannel);

        final String[] aReceivedResponse1 = { "" };
        Requester.registerResponseMessageReceiver(new EventHandler<TypedResponseReceivedEventArgs<String>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<String> y)
            {
                aReceivedResponse1[0] = y.getResponseMessage();
            }
        }, String.class);
        
        final CustomClass[] aReceivedResponse2 = { null };
        Requester.registerResponseMessageReceiver(new EventHandler<TypedResponseReceivedEventArgs<CustomClass>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<CustomClass> y)
            {
                aReceivedResponse2[0] = y.getResponseMessage();

                // Signal that the response message was received -> the loop is closed.
                aMessageReceivedEvent.set();
            }
        }, CustomClass.class);
        
        Requester.attachDuplexOutputChannel(DuplexOutputChannel);

        try
        {
            Requester.sendRequestMessage(1000, int.class);

            CustomClass aCustomRequest = new CustomClass();
            aCustomRequest.Name = "House";
            aCustomRequest.Count = 1000;
            Requester.sendRequestMessage(aCustomRequest, CustomClass.class);

            // Wait for the signal that the message is received.
            aMessageReceivedEvent.waitOne();
            //Assert.IsTrue(aMessageReceivedEvent.WaitOne(2000));
        }
        finally
        {
            Requester.detachDuplexOutputChannel();
            Responser.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals(1000, aReceivedMessage1[0]);
        assertEquals("hello", aReceivedResponse1[0]);

        assertNotNull(aReceivedMessage2[0]);
        assertEquals("House", aReceivedMessage2[0].Name);
        assertEquals(1000, aReceivedMessage2[0].Count);
    }
    
    protected IMessagingSystemFactory MessagingSystemFactory;
    protected IDuplexOutputChannel DuplexOutputChannel;
    protected IDuplexInputChannel DuplexInputChannel;

    protected IMultiTypedMessageSender Requester;
    protected IMultiTypedMessageReceiver Responser;
}

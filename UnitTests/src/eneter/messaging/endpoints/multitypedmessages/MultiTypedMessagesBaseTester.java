package eneter.messaging.endpoints.multitypedmessages;

import static org.junit.Assert.*;
import helper.EventWaitHandleExt;

import java.io.Serializable;

import org.junit.Test;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.EventHandler;
import eneter.net.system.threading.internal.AutoResetEvent;


public abstract class MultiTypedMessagesBaseTester
{
    public static class CustomClass implements Serializable
    {
        public String Name;
        public int Count;
        
        private static final long serialVersionUID = 6245713373722869822L;
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
        
        assertNotNull(aReceivedResponse2[0]);
        assertEquals("Car", aReceivedResponse2[0].Name);
        assertEquals(100, aReceivedResponse2[0].Count);
    }
    
    @Test
    public void SendReceive_Message_PerClientSerializer() throws Exception
    {
        final String[] aClient1Id = { null };
        
        IMultiTypedMessagesFactory aSender1Factory = new MultiTypedMessagesFactory(new XmlStringSerializer());
        IMultiTypedMessagesFactory aSender2Factory = new MultiTypedMessagesFactory(new JavaBinarySerializer());
        IMultiTypedMessagesFactory aReceiverFactory = new MultiTypedMessagesFactory()
            .setSerializerProvider(new GetSerializerCallback()
            {
                @Override
                public ISerializer invoke(String responseReceiverId)
                {
                    return responseReceiverId.equals(aClient1Id[0]) ? new XmlStringSerializer() : new JavaBinarySerializer();
                }
            });

        
        IMultiTypedMessageSender aSender1 = aSender1Factory.createMultiTypedMessageSender();
        IMultiTypedMessageSender aSender2 = aSender2Factory.createMultiTypedMessageSender();
        final IMultiTypedMessageReceiver aReceiver = aReceiverFactory.createMultiTypedMessageReceiver();
        aReceiver.responseReceiverConnected().subscribe(new EventHandler<ResponseReceiverEventArgs>()
        {
            @Override
            public void onEvent(Object sender, ResponseReceiverEventArgs e)
            {
                if (aClient1Id[0] == null)
                {
                    aClient1Id[0] = e.getResponseReceiverId();
                }
            }
        });
        
        final int[] aReceivedMessage1 = { 0 };
        aReceiver.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
                aReceivedMessage1[0] = y.getRequestMessage();
                
                // Send the response
                try
                {
                    aReceiver.sendResponseMessage(y.getResponseReceiverId(), "hello", String.class);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Failed to send response.", err);
                }
            }
            
        }, int.class);
        
        final CustomClass[] aReceivedMessage2 = { null };
        aReceiver.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<CustomClass>>()
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
                    aReceiver.sendResponseMessage(y.getResponseReceiverId(), aResponse, CustomClass.class);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Failed to send response.", err);
                }
            }
    
        }, CustomClass.class);
        
        aReceiver.attachDuplexInputChannel(DuplexInputChannel);

        final String[] aSender1ReceivedResponse1 = { "" };
        aSender1.registerResponseMessageReceiver(new EventHandler<TypedResponseReceivedEventArgs<String>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<String> y)
            {
                aSender1ReceivedResponse1[0] = y.getResponseMessage();
            }
        }, String.class);
        
        final AutoResetEvent aSender1MessagesReceivedEvent = new AutoResetEvent(false);
        final CustomClass[] aSender1ReceivedResponse2 = { null };
        aSender1.registerResponseMessageReceiver(new EventHandler<TypedResponseReceivedEventArgs<CustomClass>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<CustomClass> y)
            {
                aSender1ReceivedResponse2[0] = y.getResponseMessage();

                // Signal that the response message was received -> the loop is closed.
                aSender1MessagesReceivedEvent.set();
            }
        }, CustomClass.class);
        
        aSender1.attachDuplexOutputChannel(MessagingSystemFactory.createDuplexOutputChannel(DuplexInputChannel.getChannelId()));
        
        final String[] aSender2ReceivedResponse1 = { "" };
        aSender2.registerResponseMessageReceiver(new EventHandler<TypedResponseReceivedEventArgs<String>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<String> y)
            {
                aSender2ReceivedResponse1[0] = y.getResponseMessage();
            }
        }, String.class);
        
        final AutoResetEvent aSender2MessagesReceivedEvent = new AutoResetEvent(false);
        final CustomClass[] aSender2ReceivedResponse2 = { null };
        aSender2.registerResponseMessageReceiver(new EventHandler<TypedResponseReceivedEventArgs<CustomClass>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<CustomClass> y)
            {
                aSender2ReceivedResponse2[0] = y.getResponseMessage();

                // Signal that the response message was received -> the loop is closed.
                aSender2MessagesReceivedEvent.set();
            }
        }, CustomClass.class);
        
        aSender2.attachDuplexOutputChannel(MessagingSystemFactory.createDuplexOutputChannel(DuplexInputChannel.getChannelId()));
        

        try
        {
            aSender1.sendRequestMessage(1000, int.class);

            CustomClass aCustomRequest = new CustomClass();
            aCustomRequest.Name = "House";
            aCustomRequest.Count = 1000;
            aSender1.sendRequestMessage(aCustomRequest, CustomClass.class);

            aSender2.sendRequestMessage(1000, int.class);
            aSender2.sendRequestMessage(aCustomRequest, CustomClass.class);
            
            // Wait for the signal that the message is received.
            EventWaitHandleExt.waitIfNotDebugging(aSender1MessagesReceivedEvent, 2000);
            EventWaitHandleExt.waitIfNotDebugging(aSender2MessagesReceivedEvent, 2000);
            //Assert.IsTrue(aMessageReceivedEvent.WaitOne(2000));
        }
        finally
        {
            aSender1.detachDuplexOutputChannel();
            aSender2.detachDuplexOutputChannel();
            aReceiver.detachDuplexInputChannel();
        }

        // Check received values
        assertEquals(1000, aReceivedMessage1[0]);
        assertEquals("hello", aSender1ReceivedResponse1[0]);
        assertEquals("hello", aSender2ReceivedResponse1[0]);

        assertNotNull(aReceivedMessage2[0]);
        assertEquals("House", aReceivedMessage2[0].Name);
        assertEquals(1000, aReceivedMessage2[0].Count);
        
        assertNotNull(aSender1ReceivedResponse2[0]);
        assertEquals("Car", aSender1ReceivedResponse2[0].Name);
        assertEquals(100, aSender1ReceivedResponse2[0].Count);
        
        assertNotNull(aSender2ReceivedResponse2[0]);
        assertEquals("Car", aSender2ReceivedResponse2[0].Name);
        assertEquals(100, aSender2ReceivedResponse2[0].Count);
    }
    
    @Test
    public void SendReceive_NullMessage() throws Exception
    {
        // The test can be performed from more threads therefore we must synchronize.
        final AutoResetEvent aMessageReceivedEvent = new AutoResetEvent(false);

        final CustomClass[] aReceivedMessage2 = { null };
        Responser.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<CustomClass>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<CustomClass> y)
            {
                aReceivedMessage2[0] = y.getRequestMessage();

                try
                {
                    Responser.sendResponseMessage(y.getResponseReceiverId(), null, CustomClass.class);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Failed to send response.", err);
                    aMessageReceivedEvent.set();
                }
            }
    
        }, CustomClass.class);
        

        Responser.attachDuplexInputChannel(DuplexInputChannel);

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

            Requester.sendRequestMessage(null, CustomClass.class);

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
        assertNull(aReceivedMessage2[0]);
        assertNull(aReceivedResponse2[0]);
    }
    
    @Test
    public void RegisterUnregister() throws Exception
    {
        // Registering / unregistering in service.
        Responser.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<Integer> y)
            {
            }
            
        }, int.class);
        
        Responser.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<CustomClass>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<CustomClass> y)
            {
            }
    
        }, CustomClass.class);
        
        Responser.registerRequestMessageReceiver(new EventHandler<TypedRequestReceivedEventArgs<String>>()
        {
            @Override
            public void onEvent(Object x, TypedRequestReceivedEventArgs<String> y)
            {
            }
    
        }, String.class);

        assertEquals(3, Responser.getRegisteredRequestMessageTypes().size());
        assertTrue(Responser.getRegisteredRequestMessageTypes().contains(int.class));
        assertTrue(Responser.getRegisteredRequestMessageTypes().contains(CustomClass.class));
        assertTrue(Responser.getRegisteredRequestMessageTypes().contains(String.class));
        
        Responser.unregisterRequestMessageReceiver(CustomClass.class);
        
        assertEquals(2, Responser.getRegisteredRequestMessageTypes().size());
        assertTrue(Responser.getRegisteredRequestMessageTypes().contains(int.class));
        assertTrue(Responser.getRegisteredRequestMessageTypes().contains(String.class));
        
        
        // Registering / unregistering in client.
        Requester.registerResponseMessageReceiver(new EventHandler<TypedResponseReceivedEventArgs<Integer>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<Integer> y)
            {
            }
            
        }, int.class);
                
        Requester.registerResponseMessageReceiver(new EventHandler<TypedResponseReceivedEventArgs<CustomClass>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<CustomClass> y)
            {
            }
    
        }, CustomClass.class);
        
        Requester.registerResponseMessageReceiver(new EventHandler<TypedResponseReceivedEventArgs<String>>()
        {
            @Override
            public void onEvent(Object x, TypedResponseReceivedEventArgs<String> y)
            {
            }
    
        }, String.class);

        assertEquals(3, Requester.getRegisteredResponseMessageTypes().size());
        assertTrue(Requester.getRegisteredResponseMessageTypes().contains(int.class));
        assertTrue(Requester.getRegisteredResponseMessageTypes().contains(CustomClass.class));
        assertTrue(Requester.getRegisteredResponseMessageTypes().contains(String.class));
        
        Requester.unregisterResponseMessageReceiver(CustomClass.class);
        
        assertEquals(2, Requester.getRegisteredResponseMessageTypes().size());
        assertTrue(Requester.getRegisteredResponseMessageTypes().contains(int.class));
        assertTrue(Requester.getRegisteredResponseMessageTypes().contains(String.class));
    }
    
    
    protected IMessagingSystemFactory MessagingSystemFactory;
    protected IDuplexOutputChannel DuplexOutputChannel;
    protected IDuplexInputChannel DuplexInputChannel;

    protected IMultiTypedMessageSender Requester;
    protected IMultiTypedMessageReceiver Responser;
}

package service;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.composites.BufferedMonitoredMessagingFactory;
import eneter.messaging.messagingsystems.httpmessagingsystem.HttpMessagingSystemFactory;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.messaging.messagingsystems.threadpoolmessagingsystem.ThreadPoolMessagingSystemFactory;
import eneter.messaging.nodes.channelwrapper.*;
import eneter.net.system.*;

public class Calculator
{
    public static class CalculatorInputData
    {
        public double Number1;
        public double Number2;
    }

    public static class CalculatorOutputData
    {
        public double Result;
    }
    
    public Calculator() throws Exception
    {
        // We want that requests do not block each other. So every request will be processed in its own thread.
        IMessagingSystemFactory anInternalMessaging = new ThreadPoolMessagingSystemFactory();

        // We want to use Xml for serialization/deserialization.
        // Note: Alternative you can use: BinarySerializer
        ISerializer aSerializer = new GZipSerializer(new XmlStringSerializer());

        // All messages are received via one channel. So we must provide "unwrapper" forwarding incoming messages
        // to correct receivers.
        IChannelWrapperFactory aChannelWrapperFactory = new ChannelWrapperFactory(aSerializer);
        myDuplexChannelUnwrapper = aChannelWrapperFactory.createDuplexChannelUnwrapper(anInternalMessaging);

        // Factory to create message receivers.
        // Received messages will be deserialized from Xml.
        IDuplexTypedMessagesFactory aMessageReceiverFactory = new DuplexTypedMessagesFactory(aSerializer);
        
        // Create receiver to sum two numbers.
        mySumReceiver = aMessageReceiverFactory.createDuplexTypedMessageReceiver(CalculatorOutputData.class, CalculatorInputData.class);
        mySumReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<CalculatorInputData>>()
        {
            @Override
            public void onEvent(Object t1,
                    TypedRequestReceivedEventArgs<CalculatorInputData> t2)
            {
                // method handling the request
                sumCmd(t1, t2);
            }
        });
        // attach the input channel to get messages from unwrapper
        mySumReceiver.attachDuplexInputChannel(anInternalMessaging.createDuplexInputChannel("Sum"));

        // Receiver to subtract two numbers.
        mySubtractReceiver = aMessageReceiverFactory.createDuplexTypedMessageReceiver(CalculatorOutputData.class, CalculatorInputData.class);
        mySubtractReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<CalculatorInputData>>()
        {
            
            @Override
            public void onEvent(Object t1,
                    TypedRequestReceivedEventArgs<CalculatorInputData> t2)
            {
                // method handling the request
                subCmd(t1, t2);
            }
        });
        // attach the input channel to get messages from unwrapper
        mySubtractReceiver.attachDuplexInputChannel(anInternalMessaging.createDuplexInputChannel("Sub"));

        // Receiver for multiply two numbers.
        myMultiplyReceiver = aMessageReceiverFactory.createDuplexTypedMessageReceiver(CalculatorOutputData.class, CalculatorInputData.class);
        myMultiplyReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<CalculatorInputData>>()
        {
            @Override
            public void onEvent(Object t1,
                    TypedRequestReceivedEventArgs<CalculatorInputData> t2)
            {
                // method handling the request
                mulCmd(t1, t2);
            }
        });
        // attach the input channel to get messages from unwrapper
        myMultiplyReceiver.attachDuplexInputChannel(anInternalMessaging.createDuplexInputChannel("Mul"));

        // Receiver for divide two numbers.
        myDivideReceiver = aMessageReceiverFactory.createDuplexTypedMessageReceiver(CalculatorOutputData.class, CalculatorInputData.class);
        myDivideReceiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<CalculatorInputData>>()
        {
            @Override
            public void onEvent(Object t1,
                    TypedRequestReceivedEventArgs<CalculatorInputData> t2)
            {
                divCmd(t1, t2);
            }
        });
        // attach the input channel to get messages from unwrapper
        myDivideReceiver.attachDuplexInputChannel(anInternalMessaging.createDuplexInputChannel("Div"));
    }
    
    public void startCalculatorService() throws Exception
    {
        // We will use Tcp for the communication.
        IMessagingSystemFactory aServiceMessagingSystem = new BufferedMonitoredMessagingFactory(new TcpMessagingSystemFactory(),
        		60000, 1000, 6000);

        // Create input channel for the calculator receiving messages via Tcp.
        IDuplexInputChannel aGlobalInputChannel = aServiceMessagingSystem.createDuplexInputChannel("tcp://172.16.0.6:8091/");
        
        // We will use Tcp for the communication.
        //IMessagingSystemFactory aServiceMessagingSystem = new HttpMessagingSystemFactory();

        // Create input channel for the calculator receiving messages via Tcp.
        //IDuplexInputChannel aGlobalInputChannel = aServiceMessagingSystem.createDuplexInputChannel("http://172.16.0.6:8091/");

        // Attach the input channel to the unwrapper and start to listen.
        myDuplexChannelUnwrapper.attachDuplexInputChannel(aGlobalInputChannel);
    }
    
    public void stopCalculatorService() throws Exception
    {
        myDuplexChannelUnwrapper.detachDuplexInputChannel();
    }
    
    // It is called when a request to sum two numbers was received.
    private void sumCmd(Object sender, TypedRequestReceivedEventArgs<CalculatorInputData> e)
    {
        // Get input data.
        CalculatorInputData anInputData = e.getRequestMessage();

        // Calculate output result.
        CalculatorOutputData aReturn = new CalculatorOutputData();
        aReturn.Result = anInputData.Number1 + anInputData.Number2;

        System.out.println(anInputData.Number1 + " + " + anInputData.Number2 + " = " + aReturn.Result);

        // Response result to the client.
        try
        {
        	mySumReceiver.sendResponseMessage(e.getResponseReceiverId(), aReturn);
        }
        catch (Exception err)
        {
			EneterTrace.error("Sending the result failed.", err);
		}
    }
    
    // It is called when a request to subtract two numbers was received.
    private void subCmd(Object sender, TypedRequestReceivedEventArgs<CalculatorInputData> e)
    {
        // Get input data.
        CalculatorInputData anInputData = e.getRequestMessage();

        // Calculate output result.
        CalculatorOutputData aReturn = new CalculatorOutputData();
        aReturn.Result = anInputData.Number1 - anInputData.Number2;

        System.out.println(anInputData.Number1 + " - " + anInputData.Number2 + " = " + aReturn.Result);

        // Response result to the client.
        try
        {
        	mySubtractReceiver.sendResponseMessage(e.getResponseReceiverId(), aReturn);
        }
        catch (Exception err)
        {
			EneterTrace.error("Sending the result failed.", err);
		}
    }
    
    // It is called when a request to multiply two numbers was received.
    private void mulCmd(Object sender, TypedRequestReceivedEventArgs<CalculatorInputData> e)
    {
        // Get input data.
        CalculatorInputData anInputData = e.getRequestMessage();

        // Calculate output result.
        CalculatorOutputData aReturn = new CalculatorOutputData();
        aReturn.Result = anInputData.Number1 * anInputData.Number2;

        System.out.println(anInputData.Number1 + " x " + anInputData.Number2 + " = " + aReturn.Result);

        // Response result to the client.
        try
        {
        	myMultiplyReceiver.sendResponseMessage(e.getResponseReceiverId(), aReturn);
        }
        catch (Exception err)
        {
			EneterTrace.error("Sending the result failed.", err);
		}
    }
    
    // It is called when a request to divide two numbers was received.
    private void divCmd(Object sender, TypedRequestReceivedEventArgs<CalculatorInputData> e)
    {
        // Get input data.
        CalculatorInputData anInputData = e.getRequestMessage();

        // Calculate output result.
        CalculatorOutputData aReturn = new CalculatorOutputData();
        aReturn.Result = anInputData.Number1 / anInputData.Number2;

        System.out.println(anInputData.Number1 + " / " + anInputData.Number2 + " = " + aReturn.Result);

        // Response result to the client.
        try
        {
			myDivideReceiver.sendResponseMessage(e.getResponseReceiverId(), aReturn);
		}
        catch (Exception err)
        {
			EneterTrace.error("Sending the result failed.", err);
		}
    }
    
    
    private IDuplexChannelUnwrapper myDuplexChannelUnwrapper;

    private IDuplexTypedMessageReceiver<CalculatorOutputData, CalculatorInputData> mySumReceiver;
    private IDuplexTypedMessageReceiver<CalculatorOutputData, CalculatorInputData> mySubtractReceiver;
    private IDuplexTypedMessageReceiver<CalculatorOutputData, CalculatorInputData> myMultiplyReceiver;
    private IDuplexTypedMessageReceiver<CalculatorOutputData, CalculatorInputData> myDivideReceiver;
}

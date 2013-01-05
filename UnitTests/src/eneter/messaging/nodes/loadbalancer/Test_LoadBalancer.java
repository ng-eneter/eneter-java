package eneter.messaging.nodes.loadbalancer;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.threadmessagingsystem.ThreadMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class Test_LoadBalancer
{
    public static class Interval
    {
        public Interval()
        {
        }

        public Interval(double from, double to)
        {
            From = from;
            To = to;
        }

        public double From;
        public double To;
    }
    
    private static class CalculatorService
    {
        public CalculatorService(String address, IMessagingSystemFactory messaging) throws Exception
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                IDuplexTypedMessagesFactory aReceiverFactory = new DuplexTypedMessagesFactory();

                myRequestReceiver = aReceiverFactory.createDuplexTypedMessageReceiver(Double.class, Interval.class);
                myRequestReceiver.messageReceived().subscribe(myOnMessageReceivedHandler);

                IDuplexInputChannel anInputChannel = messaging.createDuplexInputChannel(address);
                myRequestReceiver.attachDuplexInputChannel(anInputChannel);
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        public void dispose()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                myRequestReceiver.detachDuplexInputChannel();
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        private void onMessageReceived(Object sender, TypedRequestReceivedEventArgs<Interval> e)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                EneterTrace.debug(String.format("Address: %s From: %f To: %f",
                    myRequestReceiver.getAttachedDuplexInputChannel().getChannelId(),
                    e.getRequestMessage().From, e.getRequestMessage().To));

                double aResult = 0.0;
                double aDx = 0.000000001;
                for (double x = e.getRequestMessage().From; x < e.getRequestMessage().To; x += aDx)
                {
                    aResult += 2 * Math.sqrt(1 - x * x) * aDx;
                }

                try
                {
                    myRequestReceiver.sendResponseMessage(e.getResponseReceiverId(), aResult);
                }
                catch (Exception err)
                {
                    EneterTrace.error("Sending of response failed.", err);
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        private IDuplexTypedMessageReceiver<Double, Interval> myRequestReceiver;
        
        private EventHandler<TypedRequestReceivedEventArgs<Interval>> myOnMessageReceivedHandler = new EventHandler<TypedRequestReceivedEventArgs<Interval>>()
        {
            @Override
            public void onEvent(Object sender, TypedRequestReceivedEventArgs<Interval> e)
            {
                onMessageReceived(sender, e);
            }
        };
    }
    
    @Test
    public void CalculatePi()
    {
        //EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
        //EneterTrace.TraceLog = new StreamWriter("d:/tracefile.txt");

        IMessagingSystemFactory aThreadMessaging = new ThreadMessagingSystemFactory();

        ArrayList<CalculatorService> aServices = new ArrayList<CalculatorService>();

        ILoadBalancer aDistributor = null;
        IDuplexTypedMessageSender<Double, Interval> aSender = null;

        // Create 50 calculating services.
        try
        {
            for (int i = 0; i < 50; ++i)
            {
                aServices.add(new CalculatorService("a" + i.ToString(), aThreadMessaging));
            }

            // Create Distributor
            ILoadBalancerFactory aDistributorFactory = new RoundRobinBalancerFactory(aThreadMessaging);
            aDistributor = aDistributorFactory.CreateLoadBalancer();

            // Attach available services to the distributor.
            for (int i = 0; i < aServices.Count; ++i)
            {
                aDistributor.AddDuplexOutputChannel("a" + i.ToString());
            }

            // Attach input channel to the distributor.
            IDuplexInputChannel anInputChannel = aThreadMessaging.CreateDuplexInputChannel("DistributorAddress");
            aDistributor.AttachDuplexInputChannel(anInputChannel);


            // Create client that needs to calculate PI.
            IDuplexTypedMessagesFactory aTypedMessagesFactory = new DuplexTypedMessagesFactory();
            aSender = aTypedMessagesFactory.CreateDuplexTypedMessageSender<double, Interval>();

            AutoResetEvent aCalculationCompletedEvent = new AutoResetEvent(false);
            int aCount = 0;
            double aPi = 0.0;
            aSender.ResponseReceived += (x, y) =>
                {
                    ++aCount;
                    EneterTrace.Debug("Completed interval: " + aCount.ToString());

                    aPi += y.ResponseMessage;

                    if (aCount == 400)
                    {
                        aCalculationCompletedEvent.Set();
                    }
                };

            IDuplexOutputChannel anOutputChannel = aThreadMessaging.CreateDuplexOutputChannel("DistributorAddress");
            aSender.AttachDuplexOutputChannel(anOutputChannel);

            // Sender sends several parallel requests to calculate specified intervals.
            // 2 / 0.005 = 400 intervals.
            for (double i = -1.0; i <= 1.0; i += 0.005)
            {
                Interval anInterval = new Interval(i, i + 0.005);
                aSender.SendRequestMessage(anInterval);
            }

            // Wait until all requests are calculated.
            EneterTrace.Debug("Test waits until completion.");
            aCalculationCompletedEvent.WaitOne();

            EneterTrace.Info("Calculated PI = " + aPi.ToString());
        }
        catch (Exception err)
        {
            EneterTrace.Error("Test failed", err);
            throw;
        }
        finally
        {
            aSender.DetachDuplexOutputChannel();
            aDistributor.DetachDuplexInputChannel();
            aServices.ForEach(x => x.Dispose());
        }
    }
}

package eneter.messaging.endpoints.rpc;

import java.lang.reflect.*;
import java.nio.channels.IllegalSelectorException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexOutputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelMessageEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.messaging.threading.dispatching.internal.SyncDispatcher;
import eneter.net.system.*;
import eneter.net.system.internal.IMethod2;
import eneter.net.system.threading.internal.ManualResetEvent;

class RpcClient<TServiceInterface> extends AttachableDuplexOutputChannelBase implements IRpcClient<TServiceInterface>
{
    // Represents the context of an active remote call.
    private class RemoteCallContext
    {
        public RemoteCallContext(String name)
        {
            myRpcCompleted = new ManualResetEvent(false);
            myMethodName = name;
        }
        
        public ManualResetEvent getRpcCompleted()
        {
            return myRpcCompleted;
        }
        
        public String getMethodName()
        {
            return myMethodName;
        }
        
        public void setError(Exception error)
        {
            myError = error;
        }
        
        public Exception getError()
        {
            return myError;
        }
        
        public void setSerializedReturnValue(Object serializedReturnValue)
        {
            mySerializedReturnValue = serializedReturnValue;
        }
        
        public Object getSerializedReturnValue()
        {
            return mySerializedReturnValue;
        }
        
        private ManualResetEvent myRpcCompleted;
        private String myMethodName;
        
        private Exception myError;
        private Object mySerializedReturnValue;
    }
    
    // Provides info about a remote event and maintains subscribers for that event.
    private class RemoteEvent
    {
        public RemoteEvent(Class<?> eventArgsType)
        {
            myEventArgsType = eventArgsType;
            mySubscribers = new ArrayList<EventHandler<?>>();
            myLock = new Object();
        }

        public Class<?> getEventArgsType()
        {
            return myEventArgsType;
        }
        
        public ArrayList<EventHandler<?>> getSubscribers()
        {
            return mySubscribers;
        }
        
        public Object getLock()
        {
            return myLock;
        }
        
        private Class<?> myEventArgsType;
        private ArrayList<EventHandler<?>> mySubscribers;
        private Object myLock;
    }
    
    
    
    
    public RpcClient(ISerializer serializer, int rpcTimeout, Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
            myRpcTimeout = rpcTimeout;

            // Dynamically implement and instantiate the given interface as the proxy.
            //Proxy = ProxyProvider.CreateInstance<TServiceInterface>(CallMethod, SubscribeEvent, UnsubscribeEvent);


            // Store remote methods.
            //foreach (MethodInfo aMethodInfo in typeof(TServiceInterface).GetMethods())
            //{
            //    Type aReturnType = aMethodInfo.ReturnType;
            //    myRemoteMethodReturnTypes[aMethodInfo.Name] = aReturnType;
            //}

            // Store remote events.
            for (Method anEventInfo : clazz.getDeclaredMethods())
            {
                // Event<MyEventArgs> somethingIsDone()
                // if it is an event.
                Class<?> aReturnType = anEventInfo.getReturnType();
                if (aReturnType == Event.class)
                {
                    // Get type of event args. 
                    Type aGenericType = anEventInfo.getGenericReturnType();
                    if (aGenericType instanceof ParameterizedType)
                    {
                        ParameterizedType aGenericParameter = (ParameterizedType) aGenericType;
                        Class<?> aGeneric = (Class<?>) aGenericParameter.getActualTypeArguments()[0];
                        
                        if (aGeneric != EventArgs.class)
                        {
                            RemoteEvent aRemoteEvent = new RemoteEvent(aGeneric);
                            myRemoteEvents.put(anEventInfo.getName(), aRemoteEvent);
                        }
                        else
                        {
                            RemoteEvent aRemoteEvent = new RemoteEvent(EventArgs.class);
                            myRemoteEvents.put(anEventInfo.getName(), aRemoteEvent);
                        }
                    }
                }
            }

            myRaiseEventInvoker = new SyncDispatcher();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public Event<DuplexChannelEventArgs> connectionOpened()
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public TServiceInterface getProxy()
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public <TEventArgs> void subscribeRemoteEvent(String eventName,
            EventHandler<TEventArgs> eventHandler)
    {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void unsubscribeRemoteEvent(String eventName,
            EventHandler<?> eventHandler)
    {
        // TODO Auto-generated method stub
        
    }
    @Override
    public Object CallRemoteMethod(String methodName, Object[] args)
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    protected void onResponseMessageReceived(Object sender,
            DuplexChannelMessageEventArgs e)
    {
        // TODO Auto-generated method stub
        
    }
    
    private Object callMethod(String methodName, Object[] parameters)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Serialize method parameters.
            Object[] aSerialzedMethodParameters = new Object[parameters.length];
            try
            {
                for (int i = 0; i < parameters.length; ++i)
                {
                    aSerialzedMethodParameters[i] = mySerializer.serialize(parameters[i], parameters[i].getClass());
                }
            }
            catch (Exception err)
            {
                string anErrorMessage = TracedObject + "failed to serialize method parameters.";
                EneterTrace.Error(anErrorMessage, err);
                throw;
            }

            // Create message asking the service to execute the method.
            RpcMessage aRequestMessage = new RpcMessage();
            aRequestMessage.Id = Interlocked.Increment(ref myCounter);
            aRequestMessage.Flag = RpcFlags.InvokeMethod;
            aRequestMessage.OperationName = methodName;
            aRequestMessage.SerializedData = aSerialzedMethodParameters;

            object aSerializedReturnValue = CallService(aRequestMessage);

            // Get the type of the return value.
            Type aReturnType;
            myRemoteMethodReturnTypes.TryGetValue(aRequestMessage.OperationName, out aReturnType);
            if (aReturnType == null)
            {
                string anErrorMessage = TracedObject + "failed to deserialize the received return value. The method '" + aRequestMessage.OperationName + "' was not found.";
                EneterTrace.Error(anErrorMessage);
                throw new InvalidOperationException(anErrorMessage);
            }

            // Deserialize the return value.
            object aDeserializedReturnValue = null;
            try
            {
                aDeserializedReturnValue = (aReturnType != typeof(void)) ?
                    mySerializer.Deserialize(aReturnType, aSerializedReturnValue) :
                    null;
            }
            catch (Exception err)
            {
                EneterTrace.Error(TracedObject + "failed to deserialize the return value.", err);
                throw;
            }

            return aDeserializedReturnValue;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void subscribeEvent(String eventName, EventHandler<?> handler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Find the event and check if it is already subscribed at the service.
            RemoteEvent aRemoteEvent = myRemoteEvents.get(eventName);
            if (aRemoteEvent == null)
            {
                String anErrorMessage = TracedObject() + "failed to subscribe. The event '" + eventName + "' does not exist.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            synchronized (aRemoteEvent.getLock())
            {
                // Store the subscriber.
                aRemoteEvent.getSubscribers().add(handler);

                // If it is the first subscriber then try to subscribe at service.
                if (aRemoteEvent.getSubscribers().size() == 1)
                {
                    if (isDuplexOutputChannelAttached() && getAttachedDuplexOutputChannel().isConnected())
                    {
                        try
                        {
                            subscribeAtService(eventName);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to subscribe at service. Eventhandler is subscribed just locally in the proxy.", err);
                        }
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void unsubscribeEvent(String eventName, EventHandler<?> handler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Find the event and check if it is already subscribed at the service.
            RemoteEvent aServiceEvent = myRemoteEvents.get(eventName);
            if (aServiceEvent == null)
            {
                EneterTrace.warning(TracedObject() + "failed to unsubscribe. The event '" + eventName + "' does not exist."); 
                return;
            }

            synchronized (aServiceEvent.getLock())
            {
                // Remove the subscriber from the list.
                aServiceEvent.getSubscribers().remove(handler);

                // If it was the last subscriber then unsubscribe at the service.
                // Note: unsubscribing from the service prevents sending of notifications across the network
                //       if nobody is subscribed.
                if (aServiceEvent.getSubscribers().isEmpty())
                {
                    // Create message asking the service to unsubscribe from the event.
                    RpcMessage aRequestMessage = new RpcMessage();
                    aRequestMessage.Id = myCounter.incrementAndGet();
                    aRequestMessage.Flag = RpcFlags.UnsubscribeEvent;
                    aRequestMessage.OperationName = eventName;

                    try
                    {
                        callService(aRequestMessage);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to unsubscribe from the service.", err);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void subscribeAtService(String eventName) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                // Create message asking the service to subscribe for the event.
                RpcMessage aSubscribeMessage = new RpcMessage();
                aSubscribeMessage.Id = myCounter.incrementAndGet();
                aSubscribeMessage.Flag = RpcFlags.SubscribeEvent;
                aSubscribeMessage.OperationName = eventName;

                // Send the subscribing request to the service.
                callService(aSubscribeMessage);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to subscribe '" + eventName + "' event at the service.", err);
                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private Object callService(RpcMessage rpcRequest) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (getAttachedDuplexOutputChannel() == null)
            {
                String anError = TracedObject() + ErrorHandler.ChannelNotAttached;
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }

            try
            {
                RemoteCallContext anRpcSyncContext = new RemoteCallContext(rpcRequest.OperationName);
                synchronized (myPendingRemoteCalls)
                {
                    myPendingRemoteCalls.put(rpcRequest.Id, anRpcSyncContext);
                }

                // Send the request.
                Object aSerializedMessage = mySerializer.serialize(rpcRequest, RpcMessage.class);
                getAttachedDuplexOutputChannel().sendMessage(aSerializedMessage);

                // Wait for the response.
                if (!anRpcSyncContext.getRpcCompleted().waitOne(myRpcTimeout))
                {
                    throw new TimeoutException("Remote call to '" + rpcRequest.OperationName + "' has not returned within the specified timeout " + myRpcTimeout + ".");
                }

                if (anRpcSyncContext.getError() != null)
                {
                    throw anRpcSyncContext.getError();
                }

                return anRpcSyncContext.getSerializedReturnValue();
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendMessageFailure, err);
                throw err;
            }
            finally
            {
                synchronized (myPendingRemoteCalls)
                {
                    myPendingRemoteCalls.remove(rpcRequest.Id);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void raiseEvent(String name, Object serializedEventArgs)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            RemoteEvent aRemoteEvent = myRemoteEvents.get(name);
            if (aRemoteEvent == null)
            {
                EneterTrace.error(TracedObject() + "failed to raise the event. The event '" + name + "' was not found.");
                return;
            }

            // Get the type of the EventArgs for the incoming event and deserialize it.
            Object anEventArgs = null;
            try
            {
                anEventArgs = (aRemoteEvent.getEventArgsType() ==  EventArgs.class) ?
                    new EventArgs() :
                    mySerializer.deserialize(serializedEventArgs, aRemoteEvent.getEventArgsType());
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize the event '" + name + "'.", err);
                return;
            }

            // Notify all subscribers.
            synchronized (aRemoteEvent.getLock())
            {
                for (EventHandler aSubscriber : aRemoteEvent.getSubscribers())
                {
                    try
                    {
                        aSubscriber.onEvent(this, anEventArgs);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notify(EventImpl<DuplexChannelEventArgs> handler, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (handler != null)
            {
                try
                {
                    handler.raise(this, e);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private ISerializer mySerializer;
    private AtomicInteger myCounter = new AtomicInteger();
    private Hashtable<Integer, RemoteCallContext> myPendingRemoteCalls = new Hashtable<Integer, RemoteCallContext>();
    private IThreadDispatcher myRaiseEventInvoker;
    private Object myConnectionManipulatorLock = new Object();
    private int myRpcTimeout;

    private Hashtable<String, Class<?>> myRemoteMethodReturnTypes = new Hashtable<String, Class<?>>();
    private Hashtable<String, RemoteEvent> myRemoteEvents = new Hashtable<String, RemoteEvent>();
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

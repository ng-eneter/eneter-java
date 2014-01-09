package eneter.messaging.endpoints.rpc;

import java.lang.reflect.*;
import java.nio.channels.IllegalSelectorException;
import java.util.*;
import java.util.Map.Entry;
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
import eneter.net.system.internal.StringExt;
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
    
    private class RemoteMethod
    {
        public RemoteMethod(Class<?> returnType, Class<?>[] argTypes)
        {
            myReturnType = returnType;
            myArgTypes = argTypes;
        }

        public Class[] getArgTypes()
        {
            return myArgTypes;
        }
        
        public Class<?> getReturnType()
        {
            return myReturnType;
        }
        
        private Class<?>[] myArgTypes;
        private Class<?> myReturnType;
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
            if (!clazz.isInterface())
            {
                String anErrorMessage = "The generic parameter TServiceInterface is not an interface.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            mySerializer = serializer;
            myRpcTimeout = rpcTimeout;

            // Dynamically implement and instantiate the given interface as the proxy.
            //Proxy = ProxyProvider.CreateInstance<TServiceInterface>(CallMethod, SubscribeEvent, UnsubscribeEvent);

            // Store remote methods and remote events.
            for (Method aMethodInfo : clazz.getDeclaredMethods())
            {
                // If it is an event.
                // Event<MyEventArgs> somethingIsDone()
                Type aGenericReturnType = aMethodInfo.getGenericReturnType();
                Class<?> aReturnType = aMethodInfo.getReturnType();
                if (aReturnType == Event.class)
                {
                    // Get type of event args. 
                    if (aGenericReturnType instanceof ParameterizedType)
                    {
                        ParameterizedType aGenericParameter = (ParameterizedType) aGenericReturnType;
                        Class<?> aGeneric = (Class<?>) aGenericParameter.getActualTypeArguments()[0];
                        
                        if (aGeneric != EventArgs.class)
                        {
                            RemoteEvent aRemoteEvent = new RemoteEvent(aGeneric);
                            myRemoteEvents.put(aMethodInfo.getName(), aRemoteEvent);
                        }
                        else
                        {
                            RemoteEvent aRemoteEvent = new RemoteEvent(EventArgs.class);
                            myRemoteEvents.put(aMethodInfo.getName(), aRemoteEvent);
                        }
                    }
                }
                // If it is a method.
                else
                {
                    // Generic return type is not supported because of generic erasure effect in Java.
                    if (aGenericReturnType instanceof ParameterizedType)
                    {
                        String anErrorMessage = TracedObject() + "does not support methods with generic return type.";
                        EneterTrace.error(anErrorMessage);
                        throw new UnsupportedOperationException(anErrorMessage);
                    }
                    
                    
                    Type[] anArguments = aMethodInfo.getGenericParameterTypes();
                    
                    // Generic parameters are not supported because of generic erasure effect in Java.
                    for (Type aParameter : anArguments)
                    {
                        if (aParameter instanceof ParameterizedType)
                        {
                            String anErrorMessage = TracedObject() + "does not support methods with generic input parameters.";
                            EneterTrace.error(anErrorMessage);
                            throw new UnsupportedOperationException(anErrorMessage);
                        }
                    }
                    
                    RemoteMethod aRemoteMethod = new RemoteMethod(aReturnType, (Class<?>[])anArguments);
                    myRemoteMethods.put(aMethodInfo.getName(), aRemoteMethod);
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
        return myConnectionOpenedEvent.getApi();
    }
    
    @Override
    public Event<DuplexChannelEventArgs> connectionClosed()
    {
        return myConnectionClosedEvent.getApi();
    }
    
    @Override
    public TServiceInterface getProxy()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void attachDuplexOutputChannel(IDuplexOutputChannel duplexOutputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                duplexOutputChannel.connectionOpened().subscribe(myOnConnectionOpened);
                duplexOutputChannel.connectionClosed().subscribe(myOnConnectionClosed);

                try
                {
                    super.attachDuplexOutputChannel(duplexOutputChannel);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to attach duplex output channel.");

                    try
                    {
                        detachDuplexOutputChannel();
                    }
                    catch (Exception err2)
                    {
                    }

                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void detachDuplexOutputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionManipulatorLock)
            {
                IDuplexOutputChannel anAttachedDuplexOutputChannel = getAttachedDuplexOutputChannel();

                super.detachDuplexOutputChannel();

                if (anAttachedDuplexOutputChannel != null)
                {
                    anAttachedDuplexOutputChannel.connectionOpened().unsubscribe(myOnConnectionOpened);
                    anAttachedDuplexOutputChannel.connectionClosed().unsubscribe(myOnConnectionClosed);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public <TEventArgs> void subscribeRemoteEvent(String eventName, EventHandler<TEventArgs> eventHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            subscribeEvent(eventName, eventHandler);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public void unsubscribeRemoteEvent(String eventName, EventHandler<?> eventHandler)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            unsubscribeEvent(eventName, eventHandler);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public Object callRemoteMethod(String methodName, Object[] args) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Object aResult = callMethod(methodName, args);
            return aResult;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onConnectionOpened(Object sender, DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Recover remote subscriptions at service.
            for (Entry<String, RemoteEvent> aRemoteEvent : myRemoteEvents.entrySet())
            {
                synchronized (aRemoteEvent.getValue().getLock())
                {
                    if (aRemoteEvent.getValue().getSubscribers().size() > 0)
                    {
                        try
                        {
                            subscribeAtService(aRemoteEvent.getKey());
                        }
                        catch (Exception err)
                        {
                            EneterTrace.error(TracedObject() + "failed to subscribe event '" + aRemoteEvent.getKey() + "' at service.", err);
                        }
                    }
                }
            }

            // Forward the event.
            notify(myConnectionOpenedEvent, e);
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
            // Forward the event.
            notify(myConnectionClosedEvent, e);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected void onResponseMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            RpcMessage aMessage = null;
            try
            {
                aMessage = mySerializer.deserialize(e.getMessage(), RpcMessage.class);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize incoming message.", err);
                return;
            }

            // If it is a response for a call.
            if (aMessage.Flag == RpcFlags.MethodResponse)
            {
                // Try to find if there is a pending request waiting for the response.
                RemoteCallContext anRpcContext;
                synchronized (myPendingRemoteCalls)
                {
                    anRpcContext = myPendingRemoteCalls.get(aMessage.Id);
                }

                if (anRpcContext != null)
                {
                    if (StringExt.isNullOrEmpty(aMessage.Error))
                    {
                        if (aMessage.SerializedData != null && aMessage.SerializedData.length > 0)
                        {
                            anRpcContext.setSerializedReturnValue(aMessage.SerializedData[0]);
                        }
                        else
                        {
                            anRpcContext.setSerializedReturnValue(null);
                        }
                    }
                    else
                    {
                        IllegalStateException anException = new IllegalStateException("Detected exception from the service:\n" + aMessage.Error);
                        anRpcContext.setError(anException);
                    }

                    // Release the pending request.
                    anRpcContext.getRpcCompleted().set();
                }
            }
            else if (aMessage.Flag == RpcFlags.RaiseEvent)
            {
                if (aMessage.SerializedData != null && aMessage.SerializedData.length > 0)
                {
                    final String anOpertationName = aMessage.OperationName;
                    final Object aSerializedData = aMessage.SerializedData[0];
                    
                    // Try to raise an event.
                    // The event is raised in its own thread so that the receiving thread is not blocked.
                    myRaiseEventInvoker.invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            raiseEvent(anOpertationName, aSerializedData);
                        }
                    });
                }
                else
                {
                    final String anOpertationName = aMessage.OperationName;
                    
                    // Note: this happens if the event is of type EventErgs.
                    // The event is raised in its own thread so that the receiving thread is not blocked.
                    myRaiseEventInvoker.invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            raiseEvent(anOpertationName, null);
                        }
                    });
                }
            }
            else
            {
                EneterTrace.warning(TracedObject() + "detected a message with unknown flag number.");
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    private Object callMethod(String methodName, Object[] parameters) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            RemoteMethod aRemoteMethod = myRemoteMethods.get(methodName);
            if (aRemoteMethod == null)
            {
                String anErrorMessage = TracedObject() + "failed to call remote method '" + methodName + "' because the method is not declared in the service interface on the client side.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            // Serialize method parameters.
            Object[] aSerialzedMethodParameters = new Object[parameters.length];
            try
            {
                for (int i = 0; i < parameters.length; ++i)
                {
                    aSerialzedMethodParameters[i] = mySerializer.serialize(parameters[i], aRemoteMethod.getArgTypes()[i]);
                }
            }
            catch (Exception err)
            {
                String anErrorMessage = TracedObject() + "failed to serialize method parameters.";
                EneterTrace.error(anErrorMessage, err);
                throw err;
            }

            // Create message asking the service to execute the method.
            RpcMessage aRequestMessage = new RpcMessage();
            aRequestMessage.Id = myCounter.incrementAndGet();
            aRequestMessage.Flag = RpcFlags.InvokeMethod;
            aRequestMessage.OperationName = methodName;
            aRequestMessage.SerializedData = aSerialzedMethodParameters;

            Object aSerializedReturnValue = callService(aRequestMessage);

            // Deserialize the return value.
            Object aDeserializedReturnValue = null;
            try
            {
                aDeserializedReturnValue = (aRemoteMethod.getReturnType() != Void.class) ?
                    mySerializer.deserialize(aSerializedReturnValue, aRemoteMethod.getReturnType()) :
                    null;
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize the return value.", err);
                throw err;
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
    private HashMap<Integer, RemoteCallContext> myPendingRemoteCalls = new HashMap<Integer, RemoteCallContext>();
    private IThreadDispatcher myRaiseEventInvoker;
    private Object myConnectionManipulatorLock = new Object();
    private int myRpcTimeout;

    private HashMap<String, RemoteMethod> myRemoteMethods = new HashMap<String, RemoteMethod>();
    private HashMap<String, RemoteEvent> myRemoteEvents = new HashMap<String, RemoteEvent>();
    
    
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEvent = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEvent = new EventImpl<DuplexChannelEventArgs>();
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionOpened = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionOpened(sender, e);
        }
    };
    
    private EventHandler<DuplexChannelEventArgs> myOnConnectionClosed = new EventHandler<DuplexChannelEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelEventArgs e)
        {
            onConnectionClosed(sender, e);
        }
    };
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexOutputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.IThreadDispatcher;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;
import eneter.net.system.threading.internal.*;


class RpcClient<TServiceInterface> extends AttachableDuplexOutputChannelBase
                implements IRpcClient<TServiceInterface>, InvocationHandler
{
    // Represents the context of an active remote call.
    private class RemoteCallContext
    {
        public RemoteCallContext()
        {
            myRpcCompleted = new ManualResetEvent(false);
        }
        
        public ManualResetEvent getRpcCompleted()
        {
            return myRpcCompleted;
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

        public Class<?>[] getArgTypes()
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
    private class RemoteEvent implements Event<Object>
    {
        public RemoteEvent(String eventName, Class<?> eventArgsType)
        {
            myEventName = eventName;
            myEventArgsType = eventArgsType;
            mySubscribers = new ArrayList<EventHandler<?>>();
            mySubscribeUnsubscribeLock = new Object();
        }
        
        public Class<?> getEventArgsType()
        {
            return myEventArgsType;
        }
        
        @Override
        public void subscribe(EventHandler<Object> eventHandler)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                synchronized (mySubscribeUnsubscribeLock)
                {
                    mySubscribers.add(eventHandler);
                    
                    // If it is the first subscriber then try to subscribe at service.
                    if (mySubscribers.size() == 1)
                    {
                        subscribeEventAtService();
                    }
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }
        
        public void subscribeEventAtService()
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                synchronized (mySubscribeUnsubscribeLock)
                {
                    if (isDuplexOutputChannelAttached() && getAttachedDuplexOutputChannel().isConnected())
                    {
                        try
                        {
                            subscribeAtService(myEventName);
                        }
                        catch (Exception err)
                        {
                            EneterTrace.warning(TracedObject() + "failed to subscribe '" + myEventName + "' at service. Eventhandler stays subscribed just locally in the proxy.", err);
                        }
                    }
                }
            }
            finally
            {
                EneterTrace.leaving(aTrace);
            }
        }

        @Override
        public void unsubscribe(EventHandler<Object> eventHandler)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                synchronized (mySubscribeUnsubscribeLock)
                {
                    mySubscribers.remove(eventHandler);
                    
                    // If it was the last subscriber then unsubscribe at the service.
                    // Note: unsubscribing from the service prevents sending of notifications across the network
                    //       if nobody is subscribed.
                    if (mySubscribers.isEmpty())
                    {
                        // Create message asking the service to unsubscribe from the event.
                        RpcMessage aRequestMessage = new RpcMessage();
                        aRequestMessage.Id = myCounter.incrementAndGet();
                        aRequestMessage.Flag = RpcFlags.UnsubscribeEvent;
                        aRequestMessage.OperationName = myEventName;
    
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
        
        @SuppressWarnings("unchecked")
        public void notifySubscribers(Object sender, Object eventArgs)
        {
            EneterTrace aTrace = EneterTrace.entering();
            try
            {
                synchronized (mySubscribeUnsubscribeLock)
                {
                    for (EventHandler<?> aSubscriber : mySubscribers)
                    {
                        try
                        {
                            ((EventHandler<Object>)aSubscriber).onEvent(sender, eventArgs);
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
        
        private String myEventName;
        private Class<?> myEventArgsType;
        private ArrayList<EventHandler<?>> mySubscribers;
        private Object mySubscribeUnsubscribeLock;
    }
    
    public RpcClient(ISerializer serializer, int rpcTimeout, IThreadDispatcher threadDispatcher, Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ServiceInterfaceChecker.check(clazz);
            
            mySerializer = serializer;
            myRpcTimeout = rpcTimeout;

            // Dynamically implement and instantiate the given interface as the proxy.
            myProxy = ProxyProvider.createInstance(this, clazz);

            // Store remote methods and remote events.
            // (public methods)
            for (Method aMethodInfo : clazz.getMethods())
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
                        Class<?> anEventArgsType = (Class<?>) aGenericParameter.getActualTypeArguments()[0];
                        
                        if (anEventArgsType != EventArgs.class)
                        {
                            RemoteEvent aRemoteEvent = new RemoteEvent(aMethodInfo.getName(), anEventArgsType);
                            myRemoteEvents.put(aMethodInfo.getName(), aRemoteEvent);
                        }
                        else
                        {
                            RemoteEvent aRemoteEvent = new RemoteEvent(aMethodInfo.getName(), EventArgs.class);
                            myRemoteEvents.put(aMethodInfo.getName(), aRemoteEvent);
                        }
                    }
                }
                // If it is a method.
                else
                {
                    // Note: casting from Type[] to Class<?>[] works in Java but not in Android.
                    //       Therefore we need to cast it manually item by item.
                    Type[] anArgumentsTmp = aMethodInfo.getGenericParameterTypes();
                    Class<?>[] anArguments = new Class<?>[anArgumentsTmp.length];
                    for (int i = 0; i < anArguments.length; ++i)
                    {
                        anArguments[i] = (Class<?>) anArgumentsTmp[i];
                    }
                    
                    RemoteMethod aRemoteMethod = new RemoteMethod(aReturnType, anArguments);
                    myRemoteMethods.put(aMethodInfo.getName(), aRemoteMethod);
                }
            }

            myThreadDispatcher = threadDispatcher;
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
        return myProxy;
    }
    
    // Implements invocation handler for dynamic proxy.
    // The handler is called when a service interface method is called.
    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // If it is an event.
            // e.g. Event<MyEventArgs> somethingIsDone()
            Type aGenericReturnType = method.getGenericReturnType();
            Class<?> aReturnType = method.getReturnType();
            if (aReturnType == Event.class)
            {
                // The event args is always generic. 
                if (aGenericReturnType instanceof ParameterizedType)
                {
                    RemoteEvent aRemoteEvent = myRemoteEvents.get(method.getName());
                    if (aRemoteEvent == null)
                    {
                        String anErrorMessage = TracedObject() + "did not find the event '" + method.getName() + "'.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }
                    
                    // Returns Event<Object> interface allowing to subscribe or unsubscribe from the event.
                    return aRemoteEvent;
                }
                else
                {
                    // This path should not occur.
                    
                    String anErrorMessage = TracedObject() + "did not find the event '" + method.getName() + "'.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }
            }
            // If it is a method.
            else
            {
                Object aResult = callRemoteMethod(method.getName(), args);
                return aResult;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public void subscribeRemoteEvent(String eventName, EventHandler<?> eventHandler) throws Exception
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
    public void unsubscribeRemoteEvent(String eventName, EventHandler<?> eventHandler) throws Exception
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
    
    @Override
    protected void onConnectionOpened(Object sender, final DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Note: Following subscribing cannot block the thread processing events from duplex output channel
            //       because a deadlock can occur. Because if the lock would stop this thread other messages could not be processed.
            ThreadPool.queueUserWorkItem(new Runnable()
            {
                @Override
                public void run()
                {
                    // Recover remote subscriptions at service.
                    for (Entry<String, RemoteEvent> aRemoteEvent : myRemoteEvents.entrySet())
                    {
                        aRemoteEvent.getValue().subscribeEventAtService();
                    }
                    
                    myThreadDispatcher.invoke(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // Forward the event.
                            notifyEvent(myConnectionOpenedEvent, e);
                        }
                    });
                }
            });
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected void onConnectionClosed(Object sender, final DuplexChannelEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Forward the event.
            myThreadDispatcher.invoke(new Runnable()
            {
                @Override
                public void run()
                {
                    notifyEvent(myConnectionClosedEvent, e);
                }
            });
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
                EneterTrace.debug("RETURN FROM RPC RECEIVED");
                
                // Try to find if there is a pending request waiting for the response.
                RemoteCallContext anRpcContext;
                synchronized (myPendingRemoteCalls)
                {
                    anRpcContext = myPendingRemoteCalls.get(aMessage.Id);
                }

                if (anRpcContext != null)
                {
                    if (StringExt.isNullOrEmpty(aMessage.ErrorType))
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
                        RpcException anException = new RpcException(aMessage.ErrorMessage, aMessage.ErrorType, aMessage.ErrorDetails);
                        anRpcContext.setError(anException);
                    }

                    // Release the pending request.
                    anRpcContext.getRpcCompleted().set();
                }
            }
            else if (aMessage.Flag == RpcFlags.RaiseEvent)
            {
                EneterTrace.debug("EVENT FROM SERVICE RECEIVED");
                
                if (aMessage.SerializedData != null && aMessage.SerializedData.length > 0)
                {
                    final String anOpertationName = aMessage.OperationName;
                    final Object aSerializedData = aMessage.SerializedData[0];
                    
                    // Try to raise an event.
                    // The event is raised in its own thread so that the receiving thread is not blocked.
                    // Note: raising an event cannot block handling of response messages because it can block
                    //       processing of an RPC response for which the RPC caller thread is waiting.
                    //       And if this waiting caller thread is a thread where events are routed and if the routing
                    //       of these events is 'blocking' then a deadlock can occur.
                    //       Therefore ThreadPool is used.
                    ThreadPool.queueUserWorkItem(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            myThreadDispatcher.invoke(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    raiseEvent(anOpertationName, aSerializedData);
                                }
                            });
                        }
                    });
                }
                else
                {
                    final String anOpertationName = aMessage.OperationName;
                    
                    // Note: this happens if the event is of type EventErgs.
                    // The event is raised in its own thread so that the receiving thread is not blocked.
                    ThreadPool.queueUserWorkItem(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            myThreadDispatcher.invoke(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    raiseEvent(anOpertationName, null);
                                }
                            });
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
    

    @SuppressWarnings("unchecked")
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
            int aLength = (parameters != null) ? parameters.length : 0;
            Object[] aSerialzedMethodParameters = new Object[aLength];
            try
            {
                for (int i = 0; i < aLength; ++i)
                {
                    aSerialzedMethodParameters[i] = mySerializer.serialize(parameters[i], (Class<Object>)aRemoteMethod.getArgTypes()[i]);
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

    @SuppressWarnings("unchecked")
    private void subscribeEvent(String eventName, EventHandler<?> handler)
            throws Exception
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

            aRemoteEvent.subscribe((EventHandler<Object>) handler);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void unsubscribeEvent(String eventName, EventHandler<?> handler)
            throws Exception
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
            
            aServiceEvent.unsubscribe((EventHandler<Object>) handler);
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
                String anError = TracedObject() + ErrorHandler.FailedToSendMessageBecauseNotAttached;
                EneterTrace.error(anError);
                throw new IllegalStateException(anError);
            }

            try
            {
                RemoteCallContext anRpcSyncContext = new RemoteCallContext();
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
                EneterTrace.error(TracedObject() + ErrorHandler.FailedToSendMessage, err);
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
            aRemoteEvent.notifySubscribers(this, anEventArgs);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void notifyEvent(EventImpl<DuplexChannelEventArgs> handler, DuplexChannelEventArgs e)
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
    private IThreadDispatcher myThreadDispatcher;
    private int myRpcTimeout;
    
    private TServiceInterface myProxy;

    private HashMap<String, RemoteMethod> myRemoteMethods = new HashMap<String, RemoteMethod>();
    private HashMap<String, RemoteEvent> myRemoteEvents = new HashMap<String, RemoteEvent>();
    
    
    private EventImpl<DuplexChannelEventArgs> myConnectionOpenedEvent = new EventImpl<DuplexChannelEventArgs>();
    private EventImpl<DuplexChannelEventArgs> myConnectionClosedEvent = new EventImpl<DuplexChannelEventArgs>();
    
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

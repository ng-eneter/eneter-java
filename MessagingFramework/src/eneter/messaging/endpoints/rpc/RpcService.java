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

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;
import eneter.net.system.linq.internal.EnumerableExt;


class RpcService<TServiceInterface> extends AttachableDuplexInputChannelBase
                                    implements IRpcService<TServiceInterface>
{
    // Maintains events and subscribed clients.
    private class EventContext
    {
        public EventContext(TServiceInterface service, Method event, EventHandler<Object> handler)
        {
            myService = service;
            myEvent = event;
            myHandler = handler;
            
            mySubscribedClients = new HashSet<String>();
        }

        // Subscribes anonymous event handler in the service.
        // When an event occurs the anonymous event handler forwards the event to subscribed remote clients.
        public void subscribe() throws Exception
        {
            Object anEventObject = myEvent.invoke(myService);
            
            @SuppressWarnings("unchecked")
            Event<Object> anEvent = (Event<Object>) anEventObject;
            
            anEvent.subscribe(myHandler);
        }

        public void unsubscribe() throws Exception
        {
            Object anEventObject = myEvent.invoke(myService);
            
            @SuppressWarnings("unchecked")
            Event<Object> anEvent = (Event<Object>) anEventObject;
            
            anEvent.unsubscribe(myHandler);
        }

        public String getEventName()
        {
            return myEvent.getName();
        }
        
        public HashSet<String> getSubscribedClients()
        {
            return mySubscribedClients;
        }
        
        private TServiceInterface myService;
        private Method myEvent;
        private EventHandler<Object> myHandler;
        
        private HashSet<String> mySubscribedClients;
    }
    
    // Maintains info about a service method.
    private class ServiceMethod
    {
        public ServiceMethod(Method methodInfo)
        {
            myMethod = methodInfo;
            myInputParameterTypes = methodInfo.getParameterTypes();
        }

        public Method getMethod()
        {
            return myMethod;
        }
        
        public Class<?>[] getInputParameterTypes()
        {
            return myInputParameterTypes;
        }
        
        public Method myMethod;
        public Class<?>[] myInputParameterTypes;
    }
    
    
    public RpcService(TServiceInterface service, ISerializer serializer, Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ServiceInterfaceChecker.check(clazz);
            
            myService = service;
            myServiceClazz = clazz;
            mySerializer = serializer;

            for (Method aMethod : myServiceClazz.getMethods())
            {
                ServiceMethod aServiceMethod = new ServiceMethod(aMethod);
                myServiceMethods.put(aMethod.getName(), aServiceMethod);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected()
    {
        return myResponseReceiverConnectedEvent.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
    {
        return myResponseReceiverDisconnectedEvent.getApi();
    }

    
    @Override
    public void attachDuplexInputChannel(IDuplexInputChannel duplexInputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Find events in the service interface and subscribe to them.
            for (Method anEventInfo : myServiceClazz.getDeclaredMethods())
            {
                // If it is an event.
                // Event<MyEventArgs> somethingIsDone()
                Type aGenericReturnType = anEventInfo.getGenericReturnType();
                Class<?> aReturnType = anEventInfo.getReturnType();
                if (aReturnType == Event.class)
                {
                    // Get type of event args. 
                    if (aGenericReturnType instanceof ParameterizedType)
                    {
                        ParameterizedType aGenericParameter = (ParameterizedType) aGenericReturnType;
                        final Class<?> anEventArgsType = (Class<?>) aGenericParameter.getActualTypeArguments()[0];
                        
                        // This handler will be subscribed to events from the service.
                        // Note: for each loop create a new local variable so that the context is preserved for the Action<,> event handler.
                        //       if anEventInfo is used then the reference would be changed.
                        final Method aTmpEventInfo = anEventInfo;
                        EventHandler<Object> anEventHandler = new EventHandler<Object>()
                        {
                            // Note: parameter e is the event of type anEventArgsType.
                            //       Therefore we can suppress checking of generic type warning.  
                            @SuppressWarnings("unchecked")
                            @Override
                            public void onEvent(Object sender, Object e)
                            {
                                EneterTrace aTrace = EneterTrace.entering();
                                try
                                {
                                    String[] aSubscribedClients = null;
                                    synchronized (myServiceEvents)
                                    {
                                        EventContext anEventContextTmp = EnumerableExt.firstOrDefault(myServiceEvents, new IFunction1<Boolean, EventContext>()
                                        {
                                            @Override
                                            public Boolean invoke(EventContext x) throws Exception
                                            {
                                                return x.getEventName().equals(aTmpEventInfo.getName());
                                            }
                                        }); 
                                                
                                        if (anEventContextTmp != null)
                                        {
                                            aSubscribedClients = anEventContextTmp.getSubscribedClients().toArray(
                                                    new String[anEventContextTmp.getSubscribedClients().size()]);
                                        }
                                    }
    
                                    // If some client is subscribed.
                                    if (aSubscribedClients != null && aSubscribedClients.length > 0)
                                    {
                                        Object aSerializedEvent = null;
                                        try
                                        {
                                            // Serialize the event and send it to subscribed clients.
                                            RpcMessage anEventMessage = new RpcMessage();
                                            anEventMessage.Id = 0; // dummy - because we do not need to track it.
                                            anEventMessage.Flag = RpcFlags.RaiseEvent;
                                            anEventMessage.OperationName = aTmpEventInfo.getName();
                                            anEventMessage.SerializedData = (anEventArgsType == EventArgs.class) ?
                                                    null : // EventArgs is a known type without parameters - we do not need to serialize it.
                                                    new Object[] { mySerializer.serialize(e, (Class<Object>)anEventArgsType) };
    
                                            aSerializedEvent = mySerializer.serialize(anEventMessage, RpcMessage.class);
                                        }
                                        catch (Exception err)
                                        {
                                            EneterTrace.error(TracedObject() + "failed to serialize the event '" + aTmpEventInfo.getName() + "'.", err);
    
                                            // Note: this exception will be thrown to the delegate that raised the event.
                                            throw err;
                                        }
    
                                        // Iterate via subscribed clients and send them the event.
                                        for (String aClient : aSubscribedClients)
                                        {
                                            try
                                            {
                                                getAttachedDuplexInputChannel().sendResponseMessage(aClient, aSerializedEvent);
                                            }
                                            catch (Exception err)
                                            {
                                                EneterTrace.error(TracedObject() + "failed to send event to the client.", err);
    
                                                // Suppose the client is disconnected so unsubscribe it from all events.
                                                unsubscribeClientFromEvents(aClient);
                                            }
                                        }
                                    }
                                }
                                catch (Exception err)
                                {
                                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                                }
                                finally
                                {
                                    EneterTrace.leaving(aTrace);
                                }
                            }
                        };
                        
                        EventContext anEventContext = null;
                        try
                        {
                            anEventContext = new EventContext(myService, aTmpEventInfo, anEventHandler);
                            anEventContext.subscribe();
                        }
                        catch (Exception err)
                        {
                            String anErrorMessage = TracedObject() + "failed to attach the output channel because it failed to create EventContext.";
                            EneterTrace.error(anErrorMessage, err);
                            throw err;
                        }

                        synchronized (myServiceEvents)
                        {
                            if (!myServiceEvents.add(anEventContext))
                            {
                                String anErrorMessage = TracedObject() + "failed to attach the output channel because it failed to create the event '" + anEventInfo.getName() + "' because the event already exists.";
                                EneterTrace.error(anErrorMessage);
                                throw new IllegalStateException(anErrorMessage);
                            }
                        }
                        
                    }
                }

                
            }

            super.attachDuplexInputChannel(duplexInputChannel);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachDuplexInputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            super.detachDuplexInputChannel();

            // Clean subscription for all clients.
            synchronized (myServiceEvents)
            {
                for (EventContext anEventContext : myServiceEvents)
                {
                    try
                    {
                        anEventContext.unsubscribe();
                    }
                    catch (Exception err)
                    {
                        EneterTrace.warning(TracedObject() + "failed to unsubscribe from the event '" + anEventContext.getEventName() + "'.", err);
                    }
                }

                myServiceEvents.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    protected void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myResponseReceiverConnectedEvent.isSubscribed())
            {
                try
                {
                    myResponseReceiverConnectedEvent.raise(this, e);
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

    @Override
    protected void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Unsubscribe disconnected client from all events.
            unsubscribeClientFromEvents(e.getResponseReceiverId());

            if (myResponseReceiverDisconnectedEvent.isSubscribed())
            {
                try
                {
                    myResponseReceiverDisconnectedEvent.raise(this, e);
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

    
    @Override
    protected void onRequestMessageReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Deserialize the incoming message.
            RpcMessage aRequestMessage = null;
            try
            {
                aRequestMessage = mySerializer.deserialize(e.getMessage(), RpcMessage.class);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize incoming request message.", err);
                return;
            }

            RpcMessage aResponseMessage = new RpcMessage();
            aResponseMessage.Id = aRequestMessage.Id;
            aResponseMessage.Flag = RpcFlags.MethodResponse;

            // If it is a remote call of a method/function.
            if (aRequestMessage.Flag == RpcFlags.InvokeMethod)
            {
                EneterTrace.debug("RPC RECEIVED");
                
                // Get the method from the service that shall be invoked.
                ServiceMethod aServiceMethod = myServiceMethods.get(aRequestMessage.OperationName);
                if (aServiceMethod != null)
                {
                    if (aRequestMessage.SerializedData != null && aRequestMessage.SerializedData.length == aServiceMethod.getInputParameterTypes().length)
                    {
                        // Deserialize input parameters.
                        Object[] aDeserializedInputParameters = new Object[aServiceMethod.getInputParameterTypes().length];
                        try
                        {
                            for (int i = 0; i < aServiceMethod.getInputParameterTypes().length; ++i)
                            {
                                aDeserializedInputParameters[i] = mySerializer.deserialize(aRequestMessage.SerializedData[i], aServiceMethod.getInputParameterTypes()[i]);
                            }
                        }
                        catch (Exception err)
                        {
                            String anErrorMessage = "failed to deserialize input parameters for '" + aRequestMessage.OperationName + "'.";
                            EneterTrace.error(anErrorMessage, err);

                            aResponseMessage.Error = anErrorMessage + "\r\n" + exceptionToString(err);
                        }

                        if (StringExt.isNullOrEmpty(aResponseMessage.Error))
                        {
                            Object aResult = null;
                            try
                            {
                                // Invoke the service method.
                                aResult = aServiceMethod.getMethod().invoke(myService, aDeserializedInputParameters);
                            }
                            catch (Exception err)
                            {
                                EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);

                                // The exception will be responded to the client.
                                aResponseMessage.Error = exceptionToString(err);
                            }

                            if (StringExt.isNullOrEmpty(aResponseMessage.Error))
                            {
                                try
                                {
                                    // Serialize the result.
                                    @SuppressWarnings("unchecked")
                                    Object aSerializedReturnValue = (aServiceMethod.getMethod().getReturnType() != Void.class) ?
                                        // Note: aResult is of type aServiceMethod.getMethod().getReturnType().
                                        //       Therefore the generic type checking warning can be supressed.
                                        mySerializer.serialize(aResult, (Class<Object>)aServiceMethod.getMethod().getReturnType()) :
                                        null;
                                    
                                    aResponseMessage.SerializedData = new Object[] { aSerializedReturnValue };
                                }
                                catch (Exception err)
                                {
                                    String anErrorMessage = TracedObject() + "failed to serialize the result.";
                                    EneterTrace.error(anErrorMessage, err);

                                    aResponseMessage.Error = anErrorMessage + "\r\n" + exceptionToString(err);
                                }
                            }
                        }
                    }
                    else
                    {
                        aRequestMessage.Error = TracedObject() + "failed to process '" + aRequestMessage.OperationName + "' because it has incorrect number of input parameters.";
                        EneterTrace.error(aRequestMessage.Error);
                    }
                }
                else
                {
                    aResponseMessage.Error = "Method '" + aRequestMessage.OperationName + "' does not exist in the service.";
                    EneterTrace.error(TracedObject() + "failed to invoke the service method because the method '" + aRequestMessage.OperationName + "' was not found in the service.");
                }
            }
            // If it is a request to subscribe/unsubcribe an event.
            else if (aRequestMessage.Flag == RpcFlags.SubscribeEvent || aRequestMessage.Flag == RpcFlags.UnsubscribeEvent)
            {
                EventContext anEventContext = null;
                synchronized (myServiceEvents)
                {
                    final String anOperationName = aRequestMessage.OperationName;
                    anEventContext = EnumerableExt.firstOrDefault(myServiceEvents, new IFunction1<Boolean, EventContext>()
                    {
                        @Override
                        public Boolean invoke(EventContext x) throws Exception
                        {
                            return x.getEventName().equals(anOperationName);
                        }
                    }); 

                    if (anEventContext != null)
                    {
                        if (aRequestMessage.Flag == RpcFlags.SubscribeEvent)
                        {
                            EneterTrace.debug("SUBSCRIBE REMOTE EVENT RECEIVED");
                            
                            // Note: Events are added to the HashSet.
                            //       Therefore it is ensured each client is subscribed only once.
                            anEventContext.getSubscribedClients().add(e.getResponseReceiverId());
                        }
                        else
                        {
                            EneterTrace.debug("UNSUBSCRIBE REMOTE EVENT RECEIVED");
                            
                            anEventContext.getSubscribedClients().remove(e.getResponseReceiverId());
                        }
                    }
                }

                if (anEventContext == null)
                {
                    aResponseMessage.Error = TracedObject() + "Event '" + aRequestMessage.OperationName + "' does not exist in the service.";
                    EneterTrace.error(aResponseMessage.Error);
                }
            }
            else
            {
                aResponseMessage.Error = TracedObject() + "could not recognize the incoming request. If it is RPC, Subscribing or Unsubscribfing.";
                EneterTrace.error(aResponseMessage.Error);
            }
            

            try
            {
                // Serialize the response message.
                Object aSerializedResponse = mySerializer.serialize(aResponseMessage, RpcMessage.class);
                getAttachedDuplexInputChannel().sendResponseMessage(e.getResponseReceiverId(), aSerializedResponse);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + ErrorHandler.SendResponseFailure, err);
            }
        }
        catch (Exception err)
        {
            EneterTrace.error(TracedObject() + ErrorHandler.DetectedException);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private void unsubscribeClientFromEvents(String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myServiceEvents)
            {
                for (EventContext anEventContext : myServiceEvents)
                {
                    anEventContext.getSubscribedClients().remove(responseReceiverId);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private static String exceptionToString(Exception err)
    {
        StringBuilder aResult = new StringBuilder();

        aResult.append(err.toString());
        aResult.append(": ");
        aResult.append(err.getMessage());
        aResult.append("\r\n");
        
        StackTraceElement[] aStackTrace = err.getStackTrace();
        for (int i = 0; i < aStackTrace.length; ++i)
        {
            aResult.append(aStackTrace[i].toString());
            
            // If it is not the last element then add the next line.
            if (i < aStackTrace.length - 1)
            {
                aResult.append("\r\n");
            }
        }
        
        return aResult.toString();
    }
    
    
    private Class<TServiceInterface> myServiceClazz;
    private TServiceInterface myService;
    private ISerializer mySerializer;
    private HashSet<EventContext> myServiceEvents = new HashSet<EventContext>();
    private HashMap<String, ServiceMethod> myServiceMethods = new HashMap<String, ServiceMethod>();
    
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

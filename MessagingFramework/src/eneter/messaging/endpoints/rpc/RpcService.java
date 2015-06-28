/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.endpoints.rpc;

import java.util.*;
import java.util.Map.Entry;

import eneter.messaging.dataprocessing.serializing.GetSerializerCallback;
import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;


class RpcService<TServiceInterface> extends AttachableDuplexInputChannelBase
                                    implements IRpcService<TServiceInterface>
{
    public RpcService(TServiceInterface singletonService, ISerializer serializer, GetSerializerCallback getSerializerCallback, Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ServiceInterfaceChecker.check(clazz);
            mySingletonService = new ServiceStub<TServiceInterface>(singletonService, serializer, getSerializerCallback, clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    public RpcService(IFunction<TServiceInterface> serviceFactoryMethod, ISerializer serializer, GetSerializerCallback getSerializerCallback, Class<TServiceInterface> clazz)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ServiceInterfaceChecker.check(clazz);
            
            myServiceFactoryMethod = serviceFactoryMethod;
            mySerializer = serializer;
            myGetSerializer = getSerializerCallback;
            myServiceClazz = clazz;
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
            // If this is singleton service mode.
            if (mySingletonService != null)
            {
                mySingletonService.attachInputChannel(duplexInputChannel);
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

            // If this is singleton service mode.
            if (mySingletonService != null)
            {
                mySingletonService.detachInputChannel();
            }
            else
            {
                // If per client mode then detach all service stubs.
                synchronized (myPerConnectionServices)
                {
                    for (Entry<String, ServiceStub<TServiceInterface>> aServiceStub : myPerConnectionServices.entrySet())
                    {
                        aServiceStub.getValue().unsubscribeClientFromEvents(aServiceStub.getKey());
                        aServiceStub.getValue().detachInputChannel();
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
    protected void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // If per client mode then create service stub for connected client.
            if (myServiceFactoryMethod != null)
            {
                TServiceInterface aServiceInstanceForThisClient = myServiceFactoryMethod.invoke();
                ServiceStub<TServiceInterface> aServiceStub = new ServiceStub<TServiceInterface>(aServiceInstanceForThisClient, mySerializer, myGetSerializer, myServiceClazz);
                aServiceStub.attachInputChannel(getAttachedDuplexInputChannel());

                synchronized (myPerConnectionServices)
                {
                    myPerConnectionServices.put(e.getResponseReceiverId(), aServiceStub);
                }
            }
            
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
        catch (Exception err)
        {
            EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
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
            if (mySingletonService != null)
            {
                mySingletonService.unsubscribeClientFromEvents(e.getResponseReceiverId());
            }
            else
            {
                // If per client mode then remove service stub for the disconnected client.
                synchronized (myPerConnectionServices)
                {
                    // Unsubscribe disconnected client from all events.
                    ServiceStub<TServiceInterface> aServiceStub = myPerConnectionServices.get(e.getResponseReceiverId());
                    if (aServiceStub != null)
                    {
                        aServiceStub.unsubscribeClientFromEvents(e.getResponseReceiverId());
                        aServiceStub.detachInputChannel();
                        myPerConnectionServices.remove(e.getResponseReceiverId());
                    }
                }
            }

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
            if (mySingletonService != null)
            {
                mySingletonService.processRemoteRequest(e);
            }
            else
            {
                // If per client mode then find the service stub associated with the client and execute the
                // remote request.
                synchronized (myPerConnectionServices)
                {
                    ServiceStub<TServiceInterface> aServiceStub = myPerConnectionServices.get(e.getResponseReceiverId());
                    if (aServiceStub != null)
                    {
                        aServiceStub.processRemoteRequest(e);
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private ServiceStub<TServiceInterface> mySingletonService;
    private HashMap<String, ServiceStub<TServiceInterface>> myPerConnectionServices = new HashMap<String, ServiceStub<TServiceInterface>>();
    private ISerializer mySerializer;
    private GetSerializerCallback myGetSerializer;
    private Class<TServiceInterface> myServiceClazz;
    private IFunction<TServiceInterface> myServiceFactoryMethod;
    
    
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnectedEvent = new EventImpl<ResponseReceiverEventArgs>();
    
    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

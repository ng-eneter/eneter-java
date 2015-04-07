/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import java.util.*;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.internal.HashSetExt;
import eneter.net.system.internal.StringExt;
import eneter.net.system.linq.internal.EnumerableExt;



class MessageBus implements IMessageBus
{
    private class TClientContext
    {
        public TClientContext(String clientResponseReceiverId, String serviceId, String serviceResponseReceiverId)
        {
            myClientResponseReceiverId = clientResponseReceiverId;
            myServiceId = serviceId;
            myServiceResponseReceiverId = serviceResponseReceiverId;
        }

        public String getClientResponseReceiverId()
        {
            return myClientResponseReceiverId;
        }
        
        public String getServiceId()
        {
            return myServiceId;
        }
        
        public String getServiceResponseReceiverId()
        {
            return myServiceResponseReceiverId;
        }
        
        private String myClientResponseReceiverId;
        private String myServiceId;
        private String myServiceResponseReceiverId;
    }
    
    private class TServiceContext
    {
        public TServiceContext(String serviceId, String serviceResponseReceiverId)
        {
            myServiceId = serviceId;
            myServiceResponseReceiverId = serviceResponseReceiverId;
        }
        
        public String getServiceId()
        {
            return myServiceId;
        }
        
        public String getServiceResponseReceiverId()
        {
            return myServiceResponseReceiverId;
        }

        private String myServiceId;
        private String myServiceResponseReceiverId;
    }
    
    
    // Helper class to wrap basic input channel functionality.
    private class TConnector extends AttachableDuplexInputChannelBase
    {
        //public Event<ResponseReceiverEventArgs> responseReceiverConnected()
        //{
        //    return myResponseReceiverConnected.getApi();
        //}
        
        public Event<ResponseReceiverEventArgs> responseReceiverDisconnected()
        {
            return myResponseReceiverDisconnected.getApi();
        }
        public Event<DuplexChannelMessageEventArgs> messageReceived()
        {
            return myMessageReceived.getApi();
        }

        @Override
        protected void onRequestMessageReceived(Object sender,
                DuplexChannelMessageEventArgs e)
        {
            if (myMessageReceived.isSubscribed())
            {
                try
                {
                    myMessageReceived.raise(sender, e);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
            
        }

        @Override
        protected void onResponseReceiverConnected(Object sender,
                ResponseReceiverEventArgs e)
        {
            if (myResponseReceiverConnected.isSubscribed())
            {
                try
                {
                    myResponseReceiverConnected.raise(sender, e);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }

        @Override
        protected void onResponseReceiverDisconnected(Object sender,
                ResponseReceiverEventArgs e)
        {
            if (myResponseReceiverDisconnected.isSubscribed())
            {
                try
                {
                    myResponseReceiverDisconnected.raise(sender, e);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                }
            }
        }

        
        private EventImpl<ResponseReceiverEventArgs> myResponseReceiverConnected = new EventImpl<ResponseReceiverEventArgs>();
        private EventImpl<ResponseReceiverEventArgs> myResponseReceiverDisconnected = new EventImpl<ResponseReceiverEventArgs>();
        private EventImpl<DuplexChannelMessageEventArgs> myMessageReceived = new EventImpl<DuplexChannelMessageEventArgs>();
        
        @Override
        protected String TracedObject()
        {
            return getClass().getSimpleName() + " ";
        }
    }
    
    
    public MessageBus(ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySerializer = serializer;
            myServiceConnector = new TConnector();
            myClientConnector = new TConnector();

            myServiceConnector.responseReceiverDisconnected().subscribe(myOnServiceDisconnected);
            myServiceConnector.messageReceived().subscribe(myOnMessageFromServiceReceived);

            myClientConnector.responseReceiverDisconnected().subscribe(myOnClientDisconnected);
            myClientConnector.messageReceived().subscribe(myOnMessageFromClientReceived);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    @Override
    public Event<MessageBusServiceEventArgs> serviceRegistered()
    {
        return myServiceRegisteredEvent.getApi();
    }


    @Override
    public Event<MessageBusServiceEventArgs> serviceUnregistered()
    {
        return myServiceUnregisteredEvent.getApi();
    }


    

    @Override
    public void attachDuplexInputChannels(IDuplexInputChannel serviceInputChannel, IDuplexInputChannel clientInputChannel)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myAttachDetachLock)
            {
                myServiceConnector.attachDuplexInputChannel(serviceInputChannel);
                myClientConnector.attachDuplexInputChannel(clientInputChannel);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachDuplexInputChannels()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionLock)
            {
                myConnectedClients.clear();
                myConnectedServices.clear();
            }

            synchronized (myAttachDetachLock)
            {
                myClientConnector.detachDuplexInputChannel();
                myServiceConnector.detachDuplexInputChannel();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public String[] getConnectedServices()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized(myConnectionLock)
            {
                String[] aServices = new String[myConnectedServices.size()];
                int i = 0;
                for (TServiceContext aServiceContext : myConnectedServices)
                {
                    aServices[i] = aServiceContext.getServiceId();
                    ++i;
                }

                return aServices;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    @Override
    public void disconnectService(String serviceAddress)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            unregisterService(serviceAddress);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    // Connection with the client was closed.
    private void onClientDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            unregisterClient(e.getResponseReceiverId(), true, false);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    // A message from the client was received.
    private void onMessageFromClientReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            MessageBusMessage aMessageBusMessage;
            try
            {
                aMessageBusMessage = mySerializer.deserialize(e.getMessage(), MessageBusMessage.class);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize message from service. The service will be disconnected.", err);
                unregisterClient(e.getResponseReceiverId(), true, true);
                return;
            }

            if (aMessageBusMessage.Request == EMessageBusRequest.ConnectClient)
            {
                EneterTrace.debug("CLIENT OPENS CONNECTION TO '" + aMessageBusMessage.Id + "'.");
                registerClient(e.getResponseReceiverId(), aMessageBusMessage.Id);
            }
            else if (aMessageBusMessage.Request == EMessageBusRequest.SendRequestMessage)
            {
                forwardMessageToService(e.getResponseReceiverId(), aMessageBusMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    // Adds the client to the list of connected clients and sends open connection message to the service.
    // If the service does not exist the client is disconnected.
    private void registerClient(final String clientResponseReceiverId, final String serviceId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            boolean anIsNewClientConnected = false;
            TClientContext aClientContext = null;
            synchronized (myConnectionLock)
            {
                try
                {
                    aClientContext = EnumerableExt.firstOrDefault(myConnectedClients, new IFunction1<Boolean, TClientContext>()
                    {
                        @Override
                        public Boolean invoke(TClientContext x) throws Exception
                        {
                            return x.getClientResponseReceiverId().equals(clientResponseReceiverId);
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to connect the client because it failed to search in firstOrDefault.", err);
                    return;
                }
                        
                // If such client does not exist yet.
                if (aClientContext == null)
                {
                    TServiceContext aServiceContext = null;
                    
                    try
                    {
                        aServiceContext = EnumerableExt.firstOrDefault(myConnectedServices, new IFunction1<Boolean, TServiceContext>()
                        {
                            @Override
                            public Boolean invoke(TServiceContext x)
                                    throws Exception
                            {
                                return x.getServiceId().equals(serviceId);
                            }
                        });
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + "failed to connect the client because it failed to search in firstOrDefault.", err);
                        return;
                    }
                            
                    // If requested service exists.
                    if (aServiceContext != null)
                    {
                        aClientContext = new TClientContext(clientResponseReceiverId, serviceId, aServiceContext.getServiceResponseReceiverId());
                        myConnectedClients.add(aClientContext);
                        anIsNewClientConnected = true;
                    }
                }
            }

            if (anIsNewClientConnected)
            {
                // Send open connection message to the service.
                try
                {
                    MessageBusMessage aMessage = new MessageBusMessage(EMessageBusRequest.ConnectClient, clientResponseReceiverId, null);
                    Object aSerializedMessage = mySerializer.serialize(aMessage, MessageBusMessage.class);

                    IDuplexInputChannel anInputChannel = myServiceConnector.getAttachedDuplexInputChannel();
                    if (anInputChannel != null)
                    {
                        anInputChannel.sendResponseMessage(aClientContext.getServiceResponseReceiverId(), aSerializedMessage);
                    }
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to send open connection message to the service '" + aClientContext.getServiceId() + "'.", err);

                    // Note: The service should not be disconnected from the message bus when not available.
                    //       Because it can be "just" overloaded. So only this new client will be disconnected from the message bus.
                    unregisterClient(clientResponseReceiverId, false, true);
                }
            }
            else
            {
                EneterTrace.warning(TracedObject() + "failed to connect the client already exists. The connection will be closed.");
                unregisterClient(clientResponseReceiverId, true, true);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void unregisterClient(final String clientResponseReceiverId,
            boolean sendCloseConnectionToServiceFlag,
            boolean disconnectClientFlag)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Unregistering client. 
            final TClientContext[] aClientContext = { null };
            synchronized (myConnectionLock)
            {
                try
                {
                    HashSetExt.removeWhere(myConnectedClients, new IFunction1<Boolean, TClientContext>()
                    {
                        @Override
                        public Boolean invoke(TClientContext x) throws Exception
                        {
                            if (x.getClientResponseReceiverId().equals(clientResponseReceiverId))
                            {
                                aClientContext[0] = x;
                                return true;
                            }
    
                            return false;
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to delete the client.", err);
                }
            }

            if (aClientContext[0] != null)
            {
                if (sendCloseConnectionToServiceFlag)
                {
                    try
                    {
                        // Send close connection message to the service.
                        MessageBusMessage aMessage = new MessageBusMessage(EMessageBusRequest.DisconnectClient, aClientContext[0].getClientResponseReceiverId(), null);
                        Object aSerializedMessage = mySerializer.serialize(aMessage, MessageBusMessage.class);

                        IDuplexInputChannel anInputChannel = myServiceConnector.getAttachedDuplexInputChannel();
                        if (anInputChannel != null)
                        {
                            anInputChannel.sendResponseMessage(aClientContext[0].getServiceResponseReceiverId(), aSerializedMessage);
                        }
                    }
                    catch (Exception err)
                    {
                        String anErrorMessage = TracedObject() + ErrorHandler.FailedToCloseConnection;
                        EneterTrace.warning(anErrorMessage, err);
                    }
                }

                // Disconnecting the client.
                if (disconnectClientFlag)
                {
                    IDuplexInputChannel anInputChannel1 = myClientConnector.getAttachedDuplexInputChannel();
                    if (anInputChannel1 != null)
                    {
                        anInputChannel1.disconnectResponseReceiver(aClientContext[0].getClientResponseReceiverId());
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void forwardMessageToService(final String clientresponseReceiverId, MessageBusMessage messageFromClient)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            TClientContext aCientContext = null;
            synchronized (myConnectionLock)
            {
                try
                {
                    aCientContext = EnumerableExt.firstOrDefault(myConnectedClients, new IFunction1<Boolean, TClientContext>()
                    {
                        @Override
                        public Boolean invoke(TClientContext x) throws Exception
                        {
                            return x.getClientResponseReceiverId().equals(clientresponseReceiverId);
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to search in firstOrDefault", err);
                }
            }

            if (aCientContext != null)
            {
                // Forward the incoming message to the service.
                IDuplexInputChannel anInputChannel = myServiceConnector.getAttachedDuplexInputChannel();
                if (anInputChannel != null)
                {
                    try
                    {
                        // Add the client id into the message.
                        // Note: Because of security reasons we do not expect Ids from the client but using Ids associated with the connection session.
                        //       Otherwise it would be possible that some client could use id of another client to pretend a different client.
                        messageFromClient.Id = clientresponseReceiverId;
                        Object aSerializedMessage = mySerializer.serialize(messageFromClient, MessageBusMessage.class);

                        anInputChannel.sendResponseMessage(aCientContext.getServiceResponseReceiverId(), aSerializedMessage);
                    }
                    catch (Exception err)
                    {
                        String anErrorMessage = TracedObject() + "failed to send message to the service '" + aCientContext.getServiceId() + "'.";
                        EneterTrace.error(anErrorMessage, err);

                        unregisterService(aCientContext.getServiceResponseReceiverId());
                    }
                }
            }
            else
            {
                String anErrorMessage = TracedObject() + "failed to send message to the service because the client was not found.";
                EneterTrace.warning(anErrorMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    // Service disconnected from the message bus.
    private void onServiceDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            unregisterService(e.getResponseReceiverId());
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void onMessageFromServiceReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            MessageBusMessage aMessageBusMessage;
            try
            {
                aMessageBusMessage = mySerializer.deserialize(e.getMessage(), MessageBusMessage.class);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to deserialize message from service. The service will be disconnected.", err);
                unregisterService(e.getResponseReceiverId());
                return;
            }
            
            if (aMessageBusMessage.Request == EMessageBusRequest.RegisterService)
            {
                EneterTrace.debug("REGISTER SERVICE: " + aMessageBusMessage.Id);
                registerService(aMessageBusMessage.Id, e.getResponseReceiverId());
            }
            else if (aMessageBusMessage.Request == EMessageBusRequest.SendResponseMessage)
            {
                // Note: forward the same message - it does not have to be serialized again.
                forwardMessageToClient(aMessageBusMessage.Id, e.getResponseReceiverId(), e.getMessage());
            }
            else if (aMessageBusMessage.Request == EMessageBusRequest.DisconnectClient)
            {
                EneterTrace.debug("SERVICE DISCONNECTs CLIENT");
                unregisterClient(aMessageBusMessage.Id, false, true);
            }
            else if (aMessageBusMessage.Request == EMessageBusRequest.ConfirmClient)
            {
                EneterTrace.debug("SERVICE CONFIRMS CLIENT");
                forwardMessageToClient(aMessageBusMessage.Id, e.getResponseReceiverId(), e.getMessage());
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void registerService(final String serviceId, final String serviceResponseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            boolean anIsNewServiceRegistered = false;
            TServiceContext aServiceContext = null;
            
            synchronized (myConnectionLock)
            {
                try
                {
                    aServiceContext = EnumerableExt.firstOrDefault(myConnectedServices, new IFunction1<Boolean, TServiceContext>()
                    {
                        @Override
                        public Boolean invoke(TServiceContext x) throws Exception
                        {
                            return x.getServiceId().equals(serviceId) || x.getServiceResponseReceiverId().equals(serviceResponseReceiverId);
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to register service '" + serviceId + "' because it failed to search in firstOrDefault", err);
                    return;
                }
                        
                if (aServiceContext == null)
                {
                    aServiceContext = new TServiceContext(serviceId, serviceResponseReceiverId);
                    myConnectedServices.add(aServiceContext);
                    anIsNewServiceRegistered = true;
                }
            }
            
            if (anIsNewServiceRegistered)
            {
                if (myServiceRegisteredEvent.isSubscribed())
                {
                    try
                    {
                        MessageBusServiceEventArgs anEvent = new MessageBusServiceEventArgs(serviceId);
                        myServiceRegisteredEvent.raise(this, anEvent);
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                    }
                }
            }
            else
            {
                // If this connection has registered the same service then do nothing.
                if (aServiceContext.getServiceId().equals(serviceId) &&
                    aServiceContext.getServiceResponseReceiverId().equals(serviceResponseReceiverId))
                {
                }
                else if (!aServiceContext.getServiceId().equals(serviceId) &&
                    aServiceContext.getServiceResponseReceiverId().equals(serviceResponseReceiverId))
                {
                    EneterTrace.warning("The connection has already registered a different service '" + aServiceContext.getServiceId() + "'. Connection will be disconnected.");
                    unregisterService(serviceResponseReceiverId);
                }
                else if (aServiceContext.getServiceId().equals(serviceId) &&
                         !aServiceContext.getServiceResponseReceiverId().equals(serviceResponseReceiverId))
                {
                    EneterTrace.warning("Service '" + serviceId + "' is already registered. Connection will be disconnected.");
                    unregisterService(serviceResponseReceiverId);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    

    private void unregisterService(final String serviceResponseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            final ArrayList<String> aClientsToDisconnect = new ArrayList<String>();

            final String[] aServiceId = { null };
            synchronized (myConnectionLock)
            {
                // Remove the service.
                try
                {
                    HashSetExt.removeWhere(myConnectedServices, new IFunction1<Boolean, TServiceContext>()
                    {
                        @Override
                        public Boolean invoke(TServiceContext x) throws Exception
                        {
                            if (x.getServiceResponseReceiverId().equals(serviceResponseReceiverId))
                            {
                                aServiceId[0] = x.getServiceId();
                                return true;
                            }
    
                            return false;
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to remove the service.");
                }

                // Remove all clients connected to the service.
                try
                {
                    HashSetExt.removeWhere(myConnectedClients, new IFunction1<Boolean, TClientContext>()
                    {
                        @Override
                        public Boolean invoke(TClientContext x) throws Exception
                        {
                            if (x.getServiceResponseReceiverId().equals(serviceResponseReceiverId))
                            {
                                aClientsToDisconnect.add(x.getClientResponseReceiverId());
                                
                                // Indicate the item shall be removed.
                                return true;
                            }
    
                            return false;
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to remove clients.");
                }
            }

            // Close connections with clients.
            if (myClientConnector.isDuplexInputChannelAttached())
            {
                for (String aClientResponseReceiverId : aClientsToDisconnect)
                {
                    IDuplexInputChannel anInputChannel = myClientConnector.getAttachedDuplexInputChannel();
                    if (anInputChannel != null)
                    {
                        anInputChannel.disconnectResponseReceiver(aClientResponseReceiverId);
                    }
                }
            }

            IDuplexInputChannel anInputChannel2 = myServiceConnector.getAttachedDuplexInputChannel();
            if (anInputChannel2 != null)
            {
                anInputChannel2.disconnectResponseReceiver(serviceResponseReceiverId);
            }

            if (myServiceUnregisteredEvent.isSubscribed() && !StringExt.isNullOrEmpty(aServiceId[0]))
            {
                EneterTrace.debug("SERVICE '" + aServiceId + "' UNREGISTERED");

                try
                {
                    MessageBusServiceEventArgs anEvent = new MessageBusServiceEventArgs(aServiceId[0]);
                    myServiceUnregisteredEvent.raise(this, anEvent);
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
    
    private void forwardMessageToClient(final String clientResponseReceiverId, final String serviceResponseReceiverId, Object serializedMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Check if the requested client id has a connection with the service session which forwards the message.
            // Note: this is to prevent that a sevice sends a message to a client which is not connected to it.
            TClientContext aClientContext = null;
            synchronized (myConnectionLock)
            {
                try
                {
                    aClientContext = EnumerableExt.firstOrDefault(myConnectedClients, new IFunction1<Boolean, TClientContext>()
                    {
                        @Override
                        public Boolean invoke(TClientContext x) throws Exception
                        {
                            return x.getClientResponseReceiverId().equals(clientResponseReceiverId) && x.getServiceResponseReceiverId().equals(serviceResponseReceiverId);
                        }
                    });
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to search firstOrDefault.", err);
                }
            }

            if (aClientContext == null)
            {
                // The associated client does not exist and the message canno be sent.
                EneterTrace.warning(TracedObject() + "failed to forward the message to client because the client was not found.");
                return;
            }

            IDuplexInputChannel anInputChannel = myClientConnector.getAttachedDuplexInputChannel();
            if (anInputChannel != null)
            {
                try
                {
                    anInputChannel.sendResponseMessage(clientResponseReceiverId, serializedMessage);
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + "failed to send message to the client.";
                    EneterTrace.error(anErrorMessage, err);

                    unregisterClient(aClientContext.getClientResponseReceiverId(), true, true);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    private Object myAttachDetachLock = new Object();
    private Object myConnectionLock = new Object();

    private HashSet<TServiceContext> myConnectedServices = new HashSet<TServiceContext>();
    private HashSet<TClientContext> myConnectedClients = new HashSet<TClientContext>();
    
    private ISerializer mySerializer;
    private TConnector myServiceConnector;
    private TConnector myClientConnector;
    
    
    private EventHandler<ResponseReceiverEventArgs> myOnServiceDisconnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onServiceDisconnected(sender, e);
        }
    };
    private EventHandler<DuplexChannelMessageEventArgs> myOnMessageFromServiceReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onMessageFromServiceReceived(sender, e);
        }
    };
    
    private EventHandler<ResponseReceiverEventArgs> myOnClientDisconnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onClientDisconnected(sender, e);
        }
    };
    private EventHandler<DuplexChannelMessageEventArgs> myOnMessageFromClientReceived = new EventHandler<DuplexChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, DuplexChannelMessageEventArgs e)
        {
            onMessageFromClientReceived(sender, e);
        }
    };
    

    private EventImpl<MessageBusServiceEventArgs> myServiceRegisteredEvent = new EventImpl<MessageBusServiceEventArgs>();
    private EventImpl<MessageBusServiceEventArgs> myServiceUnregisteredEvent = new EventImpl<MessageBusServiceEventArgs>();
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

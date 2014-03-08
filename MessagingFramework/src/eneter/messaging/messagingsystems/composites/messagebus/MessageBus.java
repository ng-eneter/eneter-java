/*
 * Project: Eneter.Messaging.Framework
 * Author:  Ondrej Uzovic
 * 
 * Copyright © Ondrej Uzovic 2014
*/

package eneter.messaging.messagingsystems.composites.messagebus;

import java.util.*;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.internal.StringExt;


class MessageBus implements IMessageBus
{
    // Helper class to wrap basic input channel functionality.
    private class TConnector extends AttachableDuplexInputChannelBase
    {
        public Event<ResponseReceiverEventArgs> responseReceiverConnected()
        {
            return myResponseReceiverConnected.getApi();
        }
        
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
    
    
    public MessageBus(IProtocolFormatter<?> protocolFormatter)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myProtocolFormatter = protocolFormatter;

            myServiceConnector = new TConnector();
            myClientConnector = new TConnector();

            myServiceConnector.responseReceiverConnected().subscribe(myOnServiceConnected);
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
        return myServiceConnectedEvent.getApi();
    }


    @Override
    public Event<MessageBusServiceEventArgs> serviceUnregistered()
    {
        return myServiceDisconnectedEvent.getApi();
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
            synchronized (myAttachDetachLock)
            {
                myClientConnector.detachDuplexInputChannel();
                myServiceConnector.detachDuplexInputChannel();
                
                // Note: make sure 'lock (myAttachDetachLock)' is never used from 'lock (myConnectionsLock)'.
                synchronized (myConnectionsLock)
                {
                    myConnectedClients.clear();
                    myConnectedServices.clear();
                }
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
            synchronized(myConnectionsLock)
            {
                String[] aServices = new String[myConnectedServices.size()];
                myConnectedServices.toArray(aServices);
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
            closeConnection(myServiceConnector, serviceAddress);
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
            EneterTrace.debug("CLIENT DISCONNECTION RECEIVED");
            unregisterClient(e.getResponseReceiverId());
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
            // If the message content is string then this message contains the service to which the client
            // wants to connect. Client is supposed to send this message immediatelly after OpenConnection().
            if (e.getMessage() instanceof String)
            {
                EneterTrace.debug("CLIENT CONNECTION RECEIVED");
                
                String aServiceId = (String)e.getMessage();
                registerClient(e.getResponseReceiverId(), aServiceId);
            }
            else
            {
                EneterTrace.debug("MESSAGE FOR SERVICE RECEIVED");

                
                ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(e.getMessage());
                if (aProtocolMessage != null && aProtocolMessage.MessageType == EProtocolMessageType.MessageReceived)
                {
                    forwardMessageToService(e.getResponseReceiverId(), e.getMessage());
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
    
    // Adds the client to the list of connected clients and sends open connection message to the service.
    // If the service does not exist the client is disconnected.
    private void registerClient(String clientId, String serviceId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            boolean anIsRegistered = false;
            synchronized (myConnectionsLock)
            {
                // If such client does not exist yet then this is an open connection message.
                if (!myConnectedClients.containsKey(clientId))
                {
                    myConnectedClients.put(clientId, serviceId);
                    anIsRegistered = true;
                }
            }

            if (anIsRegistered)
            {
                // Encode open connection message and send it to the service.
                Object anOpenConnectionMessage = myProtocolFormatter.encodeOpenConnectionMessage(clientId);
                try
                {
                    myServiceConnector.getAttachedDuplexInputChannel().sendResponseMessage(serviceId, anOpenConnectionMessage);
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + "failed to send message to the service '" + serviceId + "'.";
                    EneterTrace.error(anErrorMessage, err);

                    synchronized (myConnectionsLock)
                    {
                        myConnectedClients.remove(clientId);
                    }
                    closeConnection(myClientConnector, clientId);

                    unregisterService(serviceId);
                    closeConnection(myServiceConnector, serviceId);

                    throw err;
                }

                // Confirm the connection was open.
                try
                {
                    myClientConnector.getAttachedDuplexInputChannel().sendResponseMessage(clientId, "OK");
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + "failed to confirm the connection was open.";
                    EneterTrace.error(anErrorMessage, err);

                    synchronized (myConnectionsLock)
                    {
                        myConnectedClients.remove(clientId);
                    }
                    closeConnection(myClientConnector, clientId);

                    throw err;
                }
            }
            else
            {
                String anErrorMessage = TracedObject() + "did not register the client because the client with the same id already exists.";
                EneterTrace.warning(anErrorMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    private void unregisterClient(String clientId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aServiceId;
            synchronized (myConnectionsLock)
            {
                aServiceId = myConnectedClients.get(clientId);
                myConnectedClients.remove(clientId);
            }

            if (!StringExt.isNullOrEmpty(aServiceId))
            {
                try
                {
                    // Send close connection message to the service.
                    Object aCloseConnectionMessage = myProtocolFormatter.encodeCloseConnectionMessage(clientId);
                    myServiceConnector.getAttachedDuplexInputChannel().sendResponseMessage(aServiceId, aCloseConnectionMessage);
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + ErrorHandler.CloseConnectionFailure;
                    EneterTrace.warning(anErrorMessage, err);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void forwardMessageToService(String clientId, Object encodedProtocolMessage) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            String aServiceId = null;
            synchronized (myConnectionsLock)
            {
                aServiceId = myConnectedClients.get(clientId);
            }

            if (!StringExt.isNullOrEmpty(aServiceId))
            {
                // Forward the incomming message to the service.
                try
                {
                    myServiceConnector.getAttachedDuplexInputChannel().sendResponseMessage(aServiceId, encodedProtocolMessage);
                }
                catch (Exception err)
                {
                    String anErrorMessage = TracedObject() + "failed to send message to the service '" + aServiceId + "'.";
                    EneterTrace.error(anErrorMessage, err);

                    unregisterService(aServiceId);
                    closeConnection(myServiceConnector, aServiceId);

                    throw err;
                }
            }
            else
            {
                // The client is not registered. Maybe it was closed meanwhile. So  clean it.
                String anErrorMessage = TracedObject() + "failed to send message to the service because the client has not had open connection with the message bus.";
                EneterTrace.warning(anErrorMessage);

                closeConnection(myClientConnector, clientId);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    // Service connects to the message bus.
    private void onServiceConnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            EneterTrace.debug("SERVICE CONNECTION RECEIVED");
            
            registerService(e.getResponseReceiverId());
            
            if (myServiceConnectedEvent.isSubscribed())
            {
                MessageBusServiceEventArgs anEvent = new MessageBusServiceEventArgs(e.getResponseReceiverId());
                try
                {
                    myServiceConnectedEvent.raise(this, anEvent);
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
    
    // Service disconnected from the message bus.
    private void onServiceDisconnected(Object sender, ResponseReceiverEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            EneterTrace.debug("SERVICE DISCONNECTION RECEIVED");

            
            unregisterService(e.getResponseReceiverId());
            
            if (myServiceDisconnectedEvent.isSubscribed())
            {
                MessageBusServiceEventArgs anEvent = new MessageBusServiceEventArgs(e.getResponseReceiverId());
                try
                {
                    myServiceDisconnectedEvent.raise(this, anEvent);
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
    
    private void onMessageFromServiceReceived(Object sender, DuplexChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(e.getMessage());
                if (aProtocolMessage != null)
                {
                    // A service sends a response message to a client.
                    if (aProtocolMessage.MessageType != EProtocolMessageType.Unknown)
                    {
                        EneterTrace.debug("MESSAGE FOR CLIENT RECEIVED");
                        forwardMessageToClient(aProtocolMessage.ResponseReceiverId, e.getMessage());
                    }
                    else
                    {
                        String anErrorMessage = TracedObject() + "detected incorrect message format. The service will be disconnected.";
                        EneterTrace.warning(anErrorMessage);

                        closeConnection(myServiceConnector, e.getResponseReceiverId());
                    }
                }
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to receive a message from the service. The service will be disconnected.", err);

                closeConnection(myServiceConnector, e.getResponseReceiverId());
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void registerService(String serviceId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myConnectionsLock)
            {
                myConnectedServices.add(serviceId);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void unregisterService(String serviceId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            ArrayList<String> aClientsToDisconnect = new ArrayList<String>();

            synchronized (myConnectionsLock)
            {
                // Remove the service.
                myConnectedServices.remove(serviceId);

                // Remove all clients connected to the service.
                for (Map.Entry<String, String> aClient : myConnectedClients.entrySet())
                {
                    if (aClient.getValue().equals(serviceId))
                    {
                        aClientsToDisconnect.add(aClient.getKey());
                    }
                }
                for (String aClientId : aClientsToDisconnect)
                {
                    myConnectedClients.remove(aClientId);
                }
            }

            // Close connections with clients.
            for (String aClientId : aClientsToDisconnect)
            {
                closeConnection(myClientConnector, aClientId);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void forwardMessageToClient(String clientId, Object encodedProtocolMessage) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                myClientConnector.getAttachedDuplexInputChannel().sendResponseMessage(clientId, encodedProtocolMessage);
            }
            catch (Exception err)
            {
                String anErrorMessage = TracedObject() + "failed to send message to the client.";
                EneterTrace.error(anErrorMessage, err);

                unregisterClient(clientId);
                closeConnection(myClientConnector, clientId);

                throw err;
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private void closeConnection(TConnector connector, String responseReceiverId)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            try
            {
                connector.getAttachedDuplexInputChannel().disconnectResponseReceiver(responseReceiverId);
            }
            catch (Exception err)
            {
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    private Object myAttachDetachLock = new Object();
    private Object myConnectionsLock = new Object();

    // [service id]
    private HashSet<String> myConnectedServices = new HashSet<String>();

    // [client id, service id]
    private HashMap<String, String> myConnectedClients = new HashMap<String, String>();
    
    private TConnector myServiceConnector;
    private TConnector myClientConnector;
    private IProtocolFormatter<?> myProtocolFormatter;
    
    
    private EventHandler<ResponseReceiverEventArgs> myOnServiceConnected = new EventHandler<ResponseReceiverEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ResponseReceiverEventArgs e)
        {
            onServiceConnected(sender, e);
        }
    };
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
    

    private EventImpl<MessageBusServiceEventArgs> myServiceConnectedEvent = new EventImpl<MessageBusServiceEventArgs>();
    private EventImpl<MessageBusServiceEventArgs> myServiceDisconnectedEvent = new EventImpl<MessageBusServiceEventArgs>();
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

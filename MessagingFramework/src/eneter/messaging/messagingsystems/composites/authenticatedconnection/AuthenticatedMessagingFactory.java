/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2014 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.composites.authenticatedconnection;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.threading.dispatching.*;


/**
 * Extension for authentication during connecting.
 * 
 * Here is how the authentication procedure works:
 * <ol>
 * <li>AuthenticatedDuplexOutputChannel calls getLoginMessage callback and sends the login message and
 *     sends it to AuthenticatedDuplexInputChannel.</li>
 * <li>AuthenticatedDuplexInputChannel receives the login message and calls getHandshakeMessage callback.
 *     The returned handshake message is sent to AuthenticatedDuplexOutputChannel.</li>
 * <li>AuthenticatedDuplexOutputChannel receives the handshake message and calls getHandshakeResponseMessage.
 *     The returned handshake response message is then sent to AuthenticatedDuplexInputChannel.</li>
 * <li>AuthenticatedDuplexInputChannel receives the handshake response message and calls authenticate callback.
 *     if it returns true the connection is established.</li>
 * </ol>
 * 
 * The method setAuthenticationTimeout(..) allows to specified the maximum time until the authentication must be
 * completed. If the time is exceeded the DuplexOutputChannel.openConnection(..) throws TimeoutException.
 * The default timeout is set to 30 seconds.
 * 
 * 
 * The following example shows how to authenticate the connection for TCP.<br/>
 * <br/>
 * Service implementation:
 * 
 * <pre>
 * public class Program
 * {
 *     private static class HandshakeProvider implements IGetHandshakeMessage
 *     {
 *         {@literal @}Override
 *         public Object getHandshakeMessage(String channelId, String responseReceiverId, Object loginMessage)
 *         {
 *             // Check if login is ok.
 *             if (loginMessage instanceof String)
 *             {
 *                 String aLoginName = (String) loginMessage;
 *                 if (myUsers.containsKey(aLoginName))
 *                 {
 *                     // Login is OK so generate the handshake message.
 *                     // e.g. generate GUI.
 *                     return UUID.randomUUID().toString();
 *                 }
 *             }
 *  <br/>
 *             // Login was not ok so there is not handshake message
 *             // and the connection will be closed.
 *             EneterTrace.warning("Login was not ok. The connection will be closed.");
 *             return null;
 *         }
 *     }
 *  <br/>
 *     private static class AuthenticateProvider implements IAuthenticate
 *     {
 *         {@literal @}Override
 *         public boolean authenticate(String channelId,
 *                 String responseReceiverId, Object loginMessage,
 *                 Object handshakeMessage, Object handshakeResponseMessage)
 *         {
 *             if (loginMessage instanceof String)
 *             {
 *                 // Get the password associated with the user.
 *                 String aLoginName = (String) loginMessage;
 *                 String aPassword = myUsers.get(aLoginName);
 *  <br/>
 *                 // E.g. handshake response may be encrypted original handshake message.
 *                 //      So decrypt incoming handshake response and check if it is equal to original handshake message.
 *                 try
 *                 {
 *                     ISerializer aSerializer = new AesSerializer(aPassword);
 *                     String aDecodedHandshakeResponse = aSerializer.deserialize(handshakeResponseMessage, String.class);
 *                     String anOriginalHandshake = (String) handshakeMessage;
 *                     if (anOriginalHandshake.equals(aDecodedHandshakeResponse))
 *                     {
 *                         // The handshake response is correct so the connection can be established.
 *                         return true;
 *                     }
 *                 }
 *                 catch (Exception err)
 *                 {
 *                     // Decoding of the response message failed.
 *                     // The authentication will not pass.
 *                     EneterTrace.warning("Decoding handshake message failed.", err);
 *                 }
 *             }
 *  <br/>
 *             // Authentication did not pass.
 *             EneterTrace.warning("Authentication did not pass. The connection will be closed.");
 *             return false;
 *         }
 *     }
 *  <br/>
 *     // Helper class subscribing to receive incoming messages.
 *     private static EventHandler&lt;StringRequestReceivedEventArgs&gt; myOnRequestReceived = new EventHandler&lt;StringRequestReceivedEventArgs&gt;()
 *     {
 *         {@literal @}Override
 *         public void onEvent(Object sender, StringRequestReceivedEventArgs e)
 *         {
 *             onRequestReceived(sender, e);
 *         }
 *     };
 *  <br/>
 *     // [login, password]
 *     private static HashMap&lt;String, String&gt; myUsers = new HashMap&lt;String, String&gt;();
 *     private static IDuplexStringMessageReceiver myReceiver;
 *  <br/>
 *     public static void main(String[] args) throws Exception
 *     {
 *         // Simulate users.
 *         myUsers.put("John", "password1");
 *         myUsers.put("Steve", "password2");
 *  <br/>
 *         // TCP messaging.
 *         IMessagingSystemFactory aTcpMessaging = new TcpMessagingSystemFactory();
 *  <br/>
 *         // Authenticated messaging uses TCP as the underlying messaging.
 *         HandshakeProvider aHandshakeProvider = new HandshakeProvider();
 *         AuthenticateProvider anAuthenticationProvider = new AuthenticateProvider();
 *         IMessagingSystemFactory aMessaging = new AuthenticatedMessagingFactory(aTcpMessaging, aHandshakeProvider, anAuthenticationProvider);
 *         IDuplexInputChannel anInputChannel = aMessaging.createDuplexInputChannel("tcp://127.0.0.1:8092/");
 *  <br/>
 *         // Use just simple text messages.
 *         myReceiver = new DuplexStringMessagesFactory().createDuplexStringMessageReceiver();
 *  <br/>
 *         // Subscribe to receive messages.
 *         myReceiver.requestReceived().subscribe(myOnRequestReceived);
 *  <br/>
 *         // Attach input channel and start listening.
 *         // Note: using AuthenticatedMessaging will ensure the connection will be established only
 *         //       if the authentication procedure passes.
 *         myReceiver.attachDuplexInputChannel(anInputChannel);
 *  <br/>
 *         System.out.println("Service is running. Press ENTER to stop.");
 *         new BufferedReader(new InputStreamReader(System.in)).readLine();
 *  <br/>
 *         // Detach input channel and stop listening.
 *         // Note: tis will release the listening thread.
 *         myReceiver.detachDuplexInputChannel();
 *     }
 *  <br/>
 *     private static void onRequestReceived(Object sender, StringRequestReceivedEventArgs e)
 *     {
 *         // Process the incoming message here.
 *         System.out.println(e.getRequestMessage());
 *  <br/>
 *         // Send back the response.
 *         try
 *         {
 *             myReceiver.sendResponseMessage(e.getResponseReceiverId(), "hello");
 *         }
 *         catch (Exception err)
 *         {
 *             // Sending of response message failed.
 *             // e.g. if the client disconnected meanwhile.
 *             EneterTrace.error("Sending of response failed.", err);
 *         }
 *     }
 * }
 * </pre>
 * <br/>
 * 
 * Client Implementation:
 * <pre>
 * public class Program
 * {
 *     private static class LoginProvider implements IGetLoginMessage
 *     {
 *         {@literal @}Override
 *         public Object getLoginMessage(String channelId,
 *                 String responseReceiverId)
 *         {
 *             return "John";
 *         }
 *     }
 *  <br/>
 *     private static class HandshakeResponseProvider implements IGetHandshakeResponseMessage
 *     {
 *         {@literal @}Override
 *         public Object getHandshakeResponseMessage(String channelId,
 *                 String responseReceiverId, Object handshakeMessage)
 *         {
 *             try
 *             {
 *                 // Handshake response is encoded handshake message.
 *                 ISerializer aSerializer = new AesSerializer("password1");
 *                 Object aHandshakeResponse = aSerializer.serialize((String)handshakeMessage, String.class);
 *  <br/>
 *                 return aHandshakeResponse;
 *             }
 *             catch (Exception err)
 *             {
 *                 EneterTrace.warning("Processing handshake message failed. The connection will be closed.", err);
 *             }
 *  <br/>
 *             return null;
 *         }
 *     }
 *  <br/>
 *     private static EventHandler&lt;StringResponseReceivedEventArgs&gt; myOnResponseReceived = new EventHandler&lt;StringResponseReceivedEventArgs&gt;()
 *     {
 *         {@literal @}Override
 *         public void onEvent(Object sender, StringResponseReceivedEventArgs e)
 *         {
 *             onResponseMessageReceived(sender, e);
 *         }
 *     };
 *  <br/>
 *     private static IDuplexStringMessageSender mySender;
 *  <br/>
 *     public static void main(String[] args) throws Exception
 *     {
 *         // TCP messaging.
 *         IMessagingSystemFactory aTcpMessaging = new TcpMessagingSystemFactory();
 *  <br/>
 *         // Authenticated messaging uses TCP as the underlying messaging.
 *         LoginProvider aLoginProvider = new LoginProvider();
 *         HandshakeResponseProvider aHandshakeResponseProvider = new HandshakeResponseProvider();
 *         IMessagingSystemFactory aMessaging = new AuthenticatedMessagingFactory(aTcpMessaging, aLoginProvider, aHandshakeResponseProvider);
 *         IDuplexOutputChannel anOutputChannel = aMessaging.createDuplexOutputChannel("tcp://127.0.0.1:8092/");
 *  <br/>
 *         // Use text messages.
 *         mySender = new DuplexStringMessagesFactory().createDuplexStringMessageSender();
 *  <br/>
 *         // Subscribe to receive response messages.
 *         mySender.responseReceived().subscribe(myOnResponseReceived);
 *  <br/>
 *         // Attach output channel and connect the service.
 *         mySender.attachDuplexOutputChannel(anOutputChannel);
 *  <br/>
 *         // Send a message.
 *         mySender.sendMessage("Hello");
 *  <br/>
 *         System.out.println("Client sent the message. Press ENTER to stop.");
 *         new BufferedReader(new InputStreamReader(System.in)).readLine();
 *  <br/>
 *         // Detach output channel and stop listening.
 *         // Note: it releases the tread listening to responses.
 *         mySender.detachDuplexOutputChannel();
 *     }
 *  <br/>
 *     private static void onResponseMessageReceived(Object sender, StringResponseReceivedEventArgs e)
 *     {
 *         // Process the incoming response here.
 *         System.out.println(e.getResponseMessage());
 *     }
 *  <br/>
 * }
 * </pre>
 * 
 * 
 */
public class AuthenticatedMessagingFactory implements IMessagingSystemFactory
{
    /**
     * Constructs factory that will be used only by a client.
     * 
     * The constructor takes only callbacks which are used by the client. Therefore if you use this constructor
     * you can create only duplex output channels.
     * 
     * @param underlyingMessagingSystem underlying messaging upon which the authentication will work.
     * @param getLoginMessageCallback callback returning the login message.
     * @param getHandshakeResponseMessageCallback callback returning the response message for the handshake.
     */
    public AuthenticatedMessagingFactory(IMessagingSystemFactory underlyingMessagingSystem,
            IGetLoginMessage getLoginMessageCallback,
            IGetHandshakeResponseMessage getHandshakeResponseMessageCallback)
    {
        this(underlyingMessagingSystem, getLoginMessageCallback, getHandshakeResponseMessageCallback, null, null, null);
    }
    
    public AuthenticatedMessagingFactory(IMessagingSystemFactory underlyingMessagingSystem,
            IGetHandshakeMessage getHandshakeMessageCallback,
            IAuthenticate authenticateCallback)
    {
        this(underlyingMessagingSystem, null, null, getHandshakeMessageCallback, authenticateCallback, null);
    }
    
    /**
     * Constructs factory that will be used only by a service.
     * 
     * The constructor takes only callbacks which are used by the service. Therefore if you use this constructor
     * you can create only duplex input channels. 
     *  
     * @param underlyingMessagingSystem underlying messaging upon which the authentication will work.
     * @param getHandshakeMessageCallback callback returning the handshake message.
     * @param authenticateCallback callback performing the authentication.
     */
    public AuthenticatedMessagingFactory(IMessagingSystemFactory underlyingMessagingSystem,
            IGetHandshakeMessage getHandshakeMessageCallback,
            IAuthenticate authenticateCallback,
            IHandleAuthenticationCancelled handleAuthenticationCancelledCallback)
    {
        this(underlyingMessagingSystem, null, null, getHandshakeMessageCallback, authenticateCallback, handleAuthenticationCancelledCallback);
    }
    
    /**
     * Constructs factory that can be used by client and service simultaneously.
     * 
     * If you construct the factory with this constructor you can create both duplex output channels and
     * duplex input channels.
     * 
     * @param underlyingMessagingSystem underlying messaging upon which the authentication will work.
     * @param getLoginMessageCallback returning the login message.
     * @param getHandshakeResponseMessageCallback callback returning the response message for the handshake.
     * @param getHandshakeMessageCallback callback returning the handshake message.
     * @param authenticateCallback callback performing the authentication.
     */
    public AuthenticatedMessagingFactory(IMessagingSystemFactory underlyingMessagingSystem,
            IGetLoginMessage getLoginMessageCallback,
            IGetHandshakeResponseMessage getHandshakeResponseMessageCallback,
            IGetHandshakeMessage getHandshakeMessageCallback,
            IAuthenticate authenticateCallback,
            IHandleAuthenticationCancelled handleAuthenticationCancelledCallback)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myUnderlyingMessaging = underlyingMessagingSystem;
            myAuthenticationTimeout = 30000;

            myGetLoginMessageCallback = getLoginMessageCallback;
            myGetHandShakeMessageCallback = getHandshakeMessageCallback;
            myGetHandshakeResponseMessageCallback = getHandshakeResponseMessageCallback;
            myAuthenticateCallback = authenticateCallback;
            myHandleAuthenticationCancelled = handleAuthenticationCancelledCallback;
            
            myOutputChannelThreading = new SyncDispatching();
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates duplex output channel which performs authentication procedure during opening the connection.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myGetLoginMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex output channel because the callback to get the login message is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            if (myGetHandshakeResponseMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex output channel because the callback to get the response message for handshake is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            IDuplexOutputChannel anUnderlyingOutputChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId);
            IThreadDispatcher aThreadDispatcher = myOutputChannelThreading.getDispatcher();
            return new AuthenticatedDuplexOutputChannel(anUnderlyingOutputChannel, myGetLoginMessageCallback, myGetHandshakeResponseMessageCallback, myAuthenticationTimeout, aThreadDispatcher);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates duplex output channel which performs authentication procedure during opening the connection.
     */
    @Override
    public IDuplexOutputChannel createDuplexOutputChannel(String channelId,
            String responseReceiverId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myGetLoginMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex output channel because the callback to get the login message is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            if (myGetHandshakeResponseMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex output channel because the callback to get the response message for handshake is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            IDuplexOutputChannel anUnderlyingOutputChannel = myUnderlyingMessaging.createDuplexOutputChannel(channelId, responseReceiverId);
            IThreadDispatcher aThreadDispatcher = myOutputChannelThreading.getDispatcher();
            return new AuthenticatedDuplexOutputChannel(anUnderlyingOutputChannel, myGetLoginMessageCallback, myGetHandshakeResponseMessageCallback, myAuthenticationTimeout, aThreadDispatcher);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Creates duplex input channel which performs the authentication procedure.
     */
    @Override
    public IDuplexInputChannel createDuplexInputChannel(String channelId)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (myGetHandShakeMessageCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex input channel because the callback to get the handshake message is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            if (myAuthenticateCallback == null)
            {
                String anErrorMessage = TracedObject() + "failed to create duplex input channel because the callback to verify the handshake response message is null.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }

            IDuplexInputChannel anUnderlyingInputChannel = myUnderlyingMessaging.createDuplexInputChannel(channelId);
            return new AuthenticatedDuplexInputChannel(anUnderlyingInputChannel, myGetHandShakeMessageCallback, myAuthenticateCallback, myHandleAuthenticationCancelled);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Sets maximum time until the authentication procedure must be performed.
     * The timeout is applied in duplex output channel. If the authentication is not completed within the specified time
     * TimeoutException is thrown.<br/>
     * Timeout is set to 30 seconds by default.
     *  
     * @param authenticationTimeout
     * @return
     */
    public AuthenticatedMessagingFactory setAuthenticationTimeout(long authenticationTimeout)
    {
        myAuthenticationTimeout = authenticationTimeout;
        return this;
    }
    
    /**
     * Gets maximum time until the authentication procedure must be performed.
     * @return
     */
    public long getAuthenticationTimeout()
    {
        return myAuthenticationTimeout;
    }
    
    /**
     * Sets the threading mode for the authenticated output channel.
     * 
     * When opening connection the authenticated output channel communicates with the authenticated input channel.
     * During this communication the openConnection() is blocked until the whole authentication communication is performed.
     * It means if openConnection() is called from the same thread into which the underlying duplex output channel
     * routes events the openConneciton() would get into the deadlock (because the underlying output channel would
     * route authentication messages into the same thread).<br/>
     * <br/>
     * Therefore it is possible to set the threading mode of the authenticated output channel independently.  
     * 
     * @param threadingMode threading mode for the authenticated output channel
     * @return
     */
    public AuthenticatedMessagingFactory setOutputChannelThreading(IThreadDispatcherProvider threadingMode)
    {
        myOutputChannelThreading = threadingMode;
        return this;
    }
    
    /**
     * Gets the threading mode used by authenticated output channel.
     * 
     * @return
     */
    public IThreadDispatcherProvider getOutputChannelThreading()
    {
        return myOutputChannelThreading;
    }
    
    private IMessagingSystemFactory myUnderlyingMessaging;

    private IGetLoginMessage myGetLoginMessageCallback;
    private IGetHandshakeMessage myGetHandShakeMessageCallback;
    private IGetHandshakeResponseMessage myGetHandshakeResponseMessageCallback;
    private IAuthenticate myAuthenticateCallback;
    private IThreadDispatcherProvider myOutputChannelThreading;
    private IHandleAuthenticationCancelled myHandleAuthenticationCancelled;
    
    private long myAuthenticationTimeout;
    
    
    private String TracedObject()
    {
        return getClass().getSimpleName();
    }
}

/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.diagnostic.internal;

/**
 * Internal helper class providing typical messages of Eneter Messaging Framework.
 *
 */
public final class ErrorHandler
{
    public static final String NullOrEmptyChannelId = "Channel id is null or empty string.";
    public static final String FailedToSendMessage = " failed to send the message.";

    public static final String IsAlreadyListening = " is already listening.";
    public static final String FailedToStartListening = " failed to start listening.";
    public static final String IncorrectlyStoppedListening = " incorrectly stoped the listenig.";
    public static final String NobodySubscribedForMessage = " received a message but nobody was subscribed to process it.";

    public static final String IsAlreadyConnected = " is already connected.";
    public static final String FailedToOpenConnection = " failed to open connection.";
    public static final String FailedToCloseConnection = " failed to send the message that the connection was closed.";
    public static final String FailedToSendMessageBecauseNotConnected = " cannot send the message when not connected.";

    public static final String FailedToSendMessageBecauseNotAttached = "failed to send the request message because the output channel is not attached.";
    
    public static final String FailedToSendResponseMessage = " failed to send the response message.";
    public static final String FailedToSendResponseBecauaeClientNotConnected = " cannot send the response message when not connected.";
    public static final String FailedToSendResponseBecauseNotListening = " cannot send the response message when duplex input channel is not listening.";

    public static final String FailedToDisconnectResponseReceiver = " failed to disconnect the response receiver ";
    public static final String FailedToReceiveMessage = " failed to receive the message.";
    public static final String FailedToReceiveMessageBecauseIncorrectFormat = " failed to receive the message because the message came in incorrect format.";

    public static final String InvalidUriAddress = " is not valid URI address.";

    public static final String FailedToStopThreadId = " failed to stop the thread with id ";
    public static final String FailedToAbortThread = " failed to abort the thread.";
    public static final String FailedToUnregisterMessageHandler = " failed to unregister the handler of messages.";
    public static final String FailedInListeningLoop = " failed in the loop listening to messages.";

    public static final String ProcessingHttpConnectionFailure = " detected a failure during processing Http connection.";
    public static final String ProcessingTcpConnectionFailure = " detected a failure during processing Tcp connection.";


    public static final String DetectedException = " detected an exception.";
}

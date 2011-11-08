/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.diagnostic;

/**
 * Internal helper class to trace typical messags.
 * @author Ondrej Uzovic & Martin Valach
 *
 */
public final class ErrorHandler
{
    public static final String NullOrEmptyChannelId = "Channel id is null or empty string.";
    public static final String SendMessageFailure = " failed to send the message.";

    public static final String IsAlreadyListening = " is already listening.";
    public static final String StartListeningFailure = " failed to start listening.";
    public static final String StopListeningFailure = " incorrectly stoped the listenig.";
    public static final String NobodySubscribedForMessage = " received a message but nobody was subscribed to process it.";

    public static final String IsAlreadyConnected = " is already connected.";
    public static final String OpenConnectionFailure = " failed to open connection.";
    public static final String CloseConnectionFailure = " failed to send the message that the connection was closed.";
    public static final String SendMessageNotConnectedFailure = " cannot send the message when not connected.";

    public static final String SendResponseFailure = " failed to send the response message.";
    public static final String SendResponseNotConnectedFailure = " cannot send the response message when not connected.";

    public static final String DisconnectResponseReceiverFailure = " failed to disconnect the response receiver ";
    public static final String ReceiveMessageFailure = " failed to receive the message.";
    public static final String ReceiveMessageIncorrectFormatFailure = " failed to receive the message because the message came in incorrect format.";

    public static final String InvalidUriAddress = " is not valid URI address.";

    public static final String StopThreadFailure = " failed to stop the thread with id ";
    public static final String AbortThreadFailure = " failed to abort the thread.";
    public static final String UnregisterMessageHandlerThreadFailure = " failed to unregister the handler of messages.";
    public static final String DoListeningFailure = " failed in the loop listening to messages.";

    public static final String ProcessingHttpConnectionFailure = " detected a failure during processing Http connection.";
    public static final String ProcessingTcpConnectionFailure = " detected a failure during processing Tcp connection.";


    public static final String DetectedException = " detected an exception.";
}

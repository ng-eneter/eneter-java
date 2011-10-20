package net.eneter.messaging.messagingsystems.messagingsystembase;

/**
 * The interface declares the API for the output channel.
 * The output channel is able to send messages. (one-way communication)
 *
 * @author vachix
 *
 */
public interface IOutputChannel {
	/**
	 * Returns the id of the input channel where messages are sent.
	 */
    String GetChannelId();

    /**
     * Sends the message.
     * @param message A message
     */
    void SendMessage(Object message);
}

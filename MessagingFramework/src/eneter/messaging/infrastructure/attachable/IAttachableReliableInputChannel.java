/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

/**
 * The interface declares methods to attach/detach IReliableInputChannel
 * 
 * For more details about the reliable input channel see {@link IReliableDuplexInputChannel}.
 */
public interface IAttachableReliableInputChannel
{
	/*
	/// <summary>
	/// Attaches the reliable input channel and starts listening to messages.
	/// </summary>
	/// <param name="reliableInputChannel"></param>
	void AttachReliableInputChannel(IReliableDuplexInputChannel reliableInputChannel);
	
	/// <summary>
	/// Detaches the reliable input channel and stops listening to messages.
	/// </summary>
	void DetachReliableInputChannel();
	
	/// <summary>
	/// Returns true if the reliable input channel is attached.
	/// </summary>
	bool IsReliableInputChannelAttached { get; }
	
	/// <summary>
	/// Returns attached reliable input channel.
	/// </summary>
	IReliableDuplexInputChannel AttachedReliableInputChannel { get; }
	*/
}

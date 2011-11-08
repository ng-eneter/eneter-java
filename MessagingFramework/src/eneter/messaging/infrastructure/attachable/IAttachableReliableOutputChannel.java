/**
 * Project: Eneter.Messaging.Framework
 * Author: Martin Valach, Ondrej Uzovic
 * 
 * Copyright © 2012 Martin Valach and Ondrej Uzovic
 * 
 */

package eneter.messaging.infrastructure.attachable;

/**
 * The interface declares methods to attach/detach IReliableOutputChannel
 * 
 * For more details about the reliable output channel see {@link IReliableDuplexOutputChannel}.
 */
public interface IAttachableReliableOutputChannel
{
	/*
    /// <summary>
    /// Attaches the reliable output channel and starts listening to response messages.
    /// </summary>
    /// <param name="reliableOutputChannel"></param>
    void AttachReliableOutputChannel(IReliableDuplexOutputChannel reliableOutputChannel);

    /// <summary>
    /// Detaches the reliable output channel and stops listening to response messages.
    /// </summary>
    void DetachReliableOutputChannel();

    /// <summary>
    /// Returns true if the reliable output channel is attached.
    /// </summary>
    bool IsReliableOutputChannelAttached { get; }

    /// <summary>
    /// Returns attached reliable output channel.
    /// </summary>
    IReliableDuplexOutputChannel AttachedReliableOutputChannel { get; }
    */
 }

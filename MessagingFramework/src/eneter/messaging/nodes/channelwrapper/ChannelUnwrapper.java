/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import eneter.messaging.dataprocessing.serializing.*;
import eneter.messaging.dataprocessing.wrapping.*;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.infrastructure.attachable.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;


class ChannelUnwrapper extends AttachableInputChannelBase
                       implements IChannelUnwrapper
{
    public ChannelUnwrapper(IMessagingSystemFactory outputMessagingFactory, ISerializer serializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myOutputMessagingFactory = outputMessagingFactory;
            mySerializer = serializer;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    @Override
    protected void onMessageReceived(Object sender, ChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            WrappedData aWrappedData = null;

            try
            {
                // Unwrap the incoming message.
                aWrappedData = DataWrapper.unwrap(e.getMessage(), mySerializer);
            }
            catch (Exception err)
            {
                EneterTrace.error(TracedObject() + "failed to unwrap the message.", err);
                return;
            }

            // WrappedData.AddedData represents the channel id.
            // Therefore if everything is ok then it must be string.
            if (aWrappedData.myAddedData instanceof String)
            {
                String anOutputChannelId = (String)aWrappedData.myAddedData;

                // Get the output channel according to the channel id.
                IOutputChannel anOutputChannel = myOutputMessagingFactory.createOutputChannel(anOutputChannelId);

                try
                {
                    // Send the unwrapped message.
                    anOutputChannel.sendMessage(aWrappedData.myOriginalData);
                }
                catch (Exception err)
                {
                    EneterTrace.error(TracedObject() + "failed to send the message to the output channel '" + anOutputChannelId + "'.", err);
                }
            }
            else
            {
                EneterTrace.error(TracedObject() + "detected that the unwrapped message contian the channel id as the string type.");
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }


    private IMessagingSystemFactory myOutputMessagingFactory;
    private ISerializer mySerializer;
    
    private String TracedObject()
    {
        String anInputChannelId = (getAttachedInputChannel() != null) ? getAttachedInputChannel().getChannelId() : "";
        return "The ChannelUnwrapper attached to the input channel '" + anInputChannelId + "' "; 
    }
}
/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.dispatcher;

import java.util.HashSet;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.net.system.*;
import eneter.net.system.collections.generic.HashSetExt;
import eneter.net.system.internal.IFunction1;
import eneter.net.system.linq.internal.EnumerableExt;


class Dispatcher implements IDispatcher
{

    @Override
    public void attachOutputChannel(final IOutputChannel outputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOutputChannels)
            {
                IOutputChannel anOutputChannel = EnumerableExt.firstOrDefault(myOutputChannels, new IFunction1<Boolean, IOutputChannel>()
                {
                    @Override
                    public Boolean invoke(IOutputChannel x) throws Exception
                    {
                        return x.getChannelId().equals(outputChannel.getChannelId());
                    }
                    
                });

                if (anOutputChannel != null)
                {
                    String anErrorMessage = TracedObject() + "cannot attach the output channel because the output channel with the id '" + outputChannel.getChannelId() + "' is already attached.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }

                myOutputChannels.add(outputChannel);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachOutputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOutputChannels)
            {
                myOutputChannels.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachOutputChannel(final String outputChannelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOutputChannels)
            {
                HashSetExt.removeWhere(myOutputChannels, new IFunction1<Boolean, IOutputChannel>()
                {
                    @Override
                    public Boolean invoke(IOutputChannel x) throws Exception
                    {
                        return x.getChannelId().equals(outputChannelId);
                    }
                });
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isOutputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOutputChannels)
            {
                return !myOutputChannels.isEmpty();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public Iterable<IOutputChannel> getAttachedOutputChannels()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOutputChannels)
            {
                return EnumerableExt.toList(myOutputChannels);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void attachInputChannel(final IInputChannel inputChannel) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                IInputChannel anInputChannel = EnumerableExt.firstOrDefault(myInputChannels, new IFunction1<Boolean, IInputChannel>()
                {
                    @Override
                    public Boolean invoke(IInputChannel x) throws Exception
                    {
                        return x.getChannelId().equals(inputChannel.getChannelId());
                    }
                }); 
                        
                if (anInputChannel != null)
                {
                    String anErrorMessage = TracedObject() + "cannot attach the input channel because the input channel with the id '" + inputChannel.getChannelId() + "' is already attached.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }

                myInputChannels.add(inputChannel);
                inputChannel.messageReceived().subscribe(myOnMessageReceivedHandler);

                try
                {
                    inputChannel.startListening();
                }
                catch (Exception err)
                {
                    inputChannel.messageReceived().unsubscribe(myOnMessageReceivedHandler);
                    myInputChannels.remove(anInputChannel);

                    EneterTrace.error(TracedObject() + "failed to attach the input channel '" + inputChannel.getChannelId() + "'.", err);
                    throw err;
                }
                catch (Error err)
                {
                    inputChannel.messageReceived().unsubscribe(myOnMessageReceivedHandler);
                    myInputChannels.remove(anInputChannel);

                    EneterTrace.error(TracedObject() + "failed to attach the input channel '" + inputChannel.getChannelId() + "'.", err);
                    throw err;
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public void detachInputChannel()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                for (IInputChannel anInputChannel : myInputChannels)
                {
                    anInputChannel.stopListening();
                    anInputChannel.messageReceived().unsubscribe(myOnMessageReceivedHandler);
                }

                myInputChannels.clear();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
        
    }

    @Override
    public void detachInputChannel(final String inputChannelId) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                // Note: A trick to get the channel id into the anonymouse class.
                IInputChannel anInputChannel = EnumerableExt.firstOrDefault(myInputChannels, new IFunction1<Boolean, IInputChannel>()
                {
                    @Override
                    public Boolean invoke(IInputChannel x) throws Exception
                    {
                        return x.getChannelId().equals(inputChannelId);
                    }
                });
                        
                if (anInputChannel != null)
                {
                    anInputChannel.stopListening();
                    anInputChannel.messageReceived().unsubscribe(myOnMessageReceivedHandler);

                    myInputChannels.remove(anInputChannel);
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public boolean isInputChannelAttached()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                return !myInputChannels.isEmpty();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public Iterable<IInputChannel> getAttachedInputChannels()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myInputChannels)
            {
                return EnumerableExt.toList(myInputChannels);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private void onMessageReceived(Object sender, ChannelMessageEventArgs e)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            synchronized (myOutputChannels)
            {
                // Forward the message to all output channels
                for (IOutputChannel anOuputChannel : myOutputChannels)
                {
                    try
                    {
                        anOuputChannel.sendMessage(e.getMessage());
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error(TracedObject() + "failed to send the message to the output channel '" + anOuputChannel.getChannelId() + "'.", err);
                    }
                    catch (Error err)
                    {
                        EneterTrace.error(TracedObject() + "failed to send the message to the output channel '" + anOuputChannel.getChannelId() + "'.", err);
                        throw err;
                    }
                }
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    private HashSet<IInputChannel> myInputChannels = new HashSet<IInputChannel>();
    private HashSet<IOutputChannel> myOutputChannels = new HashSet<IOutputChannel>();
    
    private EventHandler<ChannelMessageEventArgs> myOnMessageReceivedHandler = new EventHandler<ChannelMessageEventArgs>()
    {
        @Override
        public void onEvent(Object sender, ChannelMessageEventArgs e)
        {
            onMessageReceived(sender, e);
        }
    };
    
    
    private String TracedObject()
    {
        return "The Dispatcher ";
    }
}

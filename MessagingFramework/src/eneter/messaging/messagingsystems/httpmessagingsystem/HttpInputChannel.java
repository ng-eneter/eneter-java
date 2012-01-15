package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.*;
import java.net.Socket;

import eneter.messaging.diagnostic.*;
import eneter.messaging.messagingsystems.connectionprotocols.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpInputChannelBase;
import eneter.net.system.*;


class HttpInputChannel extends TcpInputChannelBase
                       implements IInputChannel
{
    
    public HttpInputChannel(String ipAddressAndPort, IProtocolFormatter<byte[]> protocolFormatter,
            IServerSecurityFactory serverSecurityFactory)
            throws Exception
    {
        super(ipAddressAndPort, serverSecurityFactory);
        
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myProtocolFormatter = protocolFormatter;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    @Override
    public Event<ChannelMessageEventArgs> messageReceived()
    {
        return myMessageReceivedEventImpl.getApi();
    }

    @Override
    protected void disconnectClients() throws IOException
    {
        // In case of HTTP, the disconnection is not applicable.
        return;
    }

    @Override
    protected void handleConnection(Socket clientSocket) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Status that will be responsed back to the client.
            // If everything is OK, the status will be changed to OK.
            String anHttpResponseStatus = "HTTP/1.1 404 Not Found\r\n\r\n";
            
            try
            {
                // Source stream.
                InputStream anInputStream = clientSocket.getInputStream();

                // Actually, we are not interested in HTTP details.
                // So skip the HTTP part and find the beginning of the ENETER message.
                // Note: The content of the HTTP message should start after empty line.
                DataInputStream aReader = new DataInputStream(anInputStream);
                while (true)
                {
                    int aValue = aReader.read();
                    
                    // End of some line
                    if (aValue == 13)
                    {
                        aValue = aReader.read();
                        if (aValue == 10)
                        {
                            // Follows empty line.
                            aValue = aReader.read();
                            if (aValue == 13)
                            {
                                aValue = aReader.read();
                                if (aValue == 10)
                                {
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (aValue == -1)
                    {
                        throw new IllegalStateException("Unexpected end of the input stream.");
                    }
                }
                    
                // Decode the incoming message.
                ProtocolMessage aProtocolMessage = myProtocolFormatter.decodeMessage(anInputStream);
                
                // The incoming message was OK.
                anHttpResponseStatus = "HTTP/1.1 200 OK\r\n\r\n";

                if (aProtocolMessage != null)
                {
                    // Put the message to the queue from where the working thread removes it to notify
                    // subscribers of the input channel.
                    // Note: therefore subscribers of the input channel are notified always in one thread.
                    myMessageProcessingThread.enqueueMessage(aProtocolMessage);
                }

            }
            finally
            {
                // The message was successfully received, so send the HTTP response.
                DataOutputStream aResponseWriter = new DataOutputStream(clientSocket.getOutputStream());
                aResponseWriter.writeBytes(anHttpResponseStatus);
                
                // Close the connection.
                clientSocket.close();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected void handleMessage(ProtocolMessage protocolMessage)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (protocolMessage.MessageType != EProtocolMessageType.MessageReceived)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.ReceiveMessageIncorrectFormatFailure);
                return;
            }
            
            if (myMessageReceivedEventImpl.isSubscribed())
            {
                try
                {
                    myMessageReceivedEventImpl.update(this, new ChannelMessageEventArgs(getChannelId(), protocolMessage.Message));
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
                }
                catch (Error err)
                {
                    EneterTrace.error(TracedObject() + ErrorHandler.DetectedException, err);
                    throw err;
                }
            }
            else
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.NobodySubscribedForMessage);
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    
    
    private IProtocolFormatter<byte[]> myProtocolFormatter;
    
    private EventImpl<ChannelMessageEventArgs> myMessageReceivedEventImpl = new EventImpl<ChannelMessageEventArgs>();
    

    @Override
    protected String TracedObject()
    {
        return "Http input channel '" + getChannelId() + "' "; 
    }

}

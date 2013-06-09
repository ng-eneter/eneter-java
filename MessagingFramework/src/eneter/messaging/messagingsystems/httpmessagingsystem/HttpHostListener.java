/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;

import eneter.messaging.dataprocessing.streaming.internal.StreamUtil;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ErrorHandler;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.internal.*;
import eneter.net.system.IFunction1;
import eneter.net.system.IMethod1;
import eneter.net.system.linq.internal.EnumerableExt;

class HttpHostListener extends HostListenerBase
{
    public HttpHostListener(InetSocketAddress address, IServerSecurityFactory securityFactory)
    {
        super(address, securityFactory);
    }
    
    @Override
    protected void handleConnection(Socket tcpClient)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Decode http request and get header fields.
            HashMap<String, String> aRegExResult = new HashMap<String, String>();
            HashMap<String, String> aHeaderFields = new HashMap<String, String>();
            HttpFormatter.decodeHttpRequest(tcpClient.getInputStream(),
                    aRegExResult,
                    aHeaderFields);
            
            String anIncomingPath = aRegExResult.get("path");
            
            // if the incoming path is the whole uri then extract the absolute path.
            String anAbsolutePath;
            try
            {
                URI anIncomingUri = new URI(anIncomingPath);
                anAbsolutePath = anIncomingUri.getPath();
            }
            catch (URISyntaxException err)
            {
                anAbsolutePath = anIncomingPath;
            }
            
            // Get handler for that path.
            URI aHandlerUri;
            final IMethod1<Object> aPathHandler;
            synchronized (myHandlers)
            {
                final String anAbsolutePathFinal = anAbsolutePath;
                Entry<URI, IMethod1<Object>> aPair =
                        EnumerableExt.firstOrDefault(myHandlers,new IFunction1<Boolean, Entry<URI, IMethod1<Object>>>()
                            {
                                @Override
                                public Boolean invoke(
                                        Entry<URI, IMethod1<Object>> x)
                                        throws Exception
                                {
                                    return x.getKey().getPath().equals(anAbsolutePathFinal);
                                }
                                
                            });
                
                aPathHandler = aPair.getValue(); 
                aHandlerUri = aPair.getKey();
            }
            
            // If the listener does not exist.
            if (aPathHandler == null)
            {
                EneterTrace.warning(TracedObject() + "does not listen to " + anIncomingPath);
                byte[] aCloseConnectionResponse = HttpFormatter.encodeError(404);
                tcpClient.getOutputStream().write(aCloseConnectionResponse, 0, aCloseConnectionResponse.length);

                return;
            }
            
            // Create context for the received request message
            URI aRequestUri = new URI(aHandlerUri.getScheme(), null, aHandlerUri.getHost(), getAddress().getPort(), anAbsolutePath, aRegExResult.get("query"), null);

            // Read the content of the request message.
            byte[] aRequestMessage = null;
            
            // If the request message comes in chunks then read chunks.
            String aChunkValue = aHeaderFields.get("Transfer-encoding");
            if (aChunkValue != null && aChunkValue.equals("chunked"))
            {
                ByteArrayOutputStream aBuffer = new ByteArrayOutputStream();
                while (true)
                {
                    byte[] aChunkData = HttpFormatter.decodeChunk(tcpClient.getInputStream());
                    if (aChunkData != null && aChunkData.length > 0)
                    {
                        aBuffer.write(aChunkData, 0, aChunkData.length);
                    }
                    else
                    {
                        // End of chunks reading.
                        break;
                    }
                }

                aRequestMessage = aBuffer.toByteArray();
            }
            else if (aRegExResult.get("method").equals("POST"))
            {
                // Get size of the message.
                String aSizeStr = aHeaderFields.get("Content-Length");
                if (aSizeStr == null)
                {
                    EneterTrace.warning(TracedObject() + "failed to receive http request. The Content-Length is missing.");
                    byte[] aCloseConnectionResponse = HttpFormatter.encodeError(400);
                    tcpClient.getOutputStream().write(aCloseConnectionResponse, 0, aCloseConnectionResponse.length);

                    return;
                }

                int aSize;
                try
                {
                    aSize = Integer.parseInt(aSizeStr);
                }
                catch (Exception err)
                {
                    EneterTrace.warning(TracedObject() + "failed to receive http request. The Content-Length was not a valid number.", err);
                    byte[] aCloseConnectionResponse = HttpFormatter.encodeError(400);
                    tcpClient.getOutputStream().write(aCloseConnectionResponse, 0, aCloseConnectionResponse.length);

                    return;
                }

                aRequestMessage =  StreamUtil.readBytes(tcpClient.getInputStream(), aSize);
            }
            
            // The message is not in chunks.
            HttpRequestContext aClientContext = new HttpRequestContext(aRequestUri, aRequestMessage, tcpClient.getOutputStream());

            try
            {
                aPathHandler.invoke(aClientContext);
            }
            catch (Exception err)
            {
                EneterTrace.warning(TracedObject() + ErrorHandler.DetectedException, err);
            }

            // If the response was not sent then sent OK response.
            if (!aClientContext.isResponded())
            {
                aClientContext.response(null);
            }
            
        }
        catch (IOException err)
        {
            EneterTrace.warning(TracedObject() + "detected closed connection.", err);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    protected String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

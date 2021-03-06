package eneter.messaging.messagingsystems.websocketmessagingsystem;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.NoneSecurityServerFactory;
import eneter.net.system.EventHandler;
import eneter.net.system.IMethod1;
import eneter.net.system.threading.internal.AutoResetEvent;

public class Test_WebSocketListener
{
    //@Test
    public void Regex()
    {
        Pattern anHttpOpenConnectionRequest = Pattern.compile(
                "(^GET\\s([^\\s\\?]+)(?:\\?[^\\s]+)?\\sHTTP\\/1\\.1\\r\\n)|" +
                "(([^:\\r\\n]+):\\s([^\\r\\n]+)\\r\\n)|" +
                "(\\r\\n)",
                Pattern.CASE_INSENSITIVE);
        
        String anHttpRequest = "GET / HTTP/1.1\r\n" +
                "Host: 127.0.0.1:8087\r\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 6.0; rv:12.0) Gecko/20100101 Firefox/12.0\r\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
                "Accept-Language: en-us,en;q=0.5\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Connection: keep-alive, Upgrade\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "Origin: null\r\n" +
                "Sec-WebSocket-Key: SnRZ/RQDovUFsalYMbLJMQ==\r\n" +
                "Pragma: no-cache\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Upgrade: websocket\r\n\r\n";
        
        Matcher aMatcher = anHttpOpenConnectionRequest.matcher(anHttpRequest);
        
        int aLineIdx = 0;
        while (aMatcher.find())
        {
            String s = aMatcher.group();
            
            if (!s.equals("\r\n"))
            {
                if (aLineIdx == 0)
                {
                    String aPath = aMatcher.group(2);
                    aPath += "";
                }
                else
                {
                    String aKey = aMatcher.group(4);
                    String aValue = aMatcher.group(5);
                    
                    aValue += "";
                }
            }
            
            ++aLineIdx;
        }
        
    }
    
    //@Test
    public void outInPipes() throws Exception
    {
        PipedOutputStream anOut = new PipedOutputStream();
        final PipedInputStream anIn = new PipedInputStream(anOut);
        
        Thread aListener = new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                ByteArrayOutputStream anOutputMemStream = new ByteArrayOutputStream();
                int aSize = 0;
                byte[] aBuff = new byte[10];
                try
                {
                    while ((aSize = anIn.read(aBuff)) > 0)
                    {
                        EneterTrace.info(Integer.toString(aSize));
                        
                        anOutputMemStream.write(aBuff, 0, aSize);
                    }
                }
                catch (IOException e)
                {
                    EneterTrace.error("", e);
                }
            }
        });

        //aListener.start();
        
        for (int i = 0; i < 10; ++i)
        {
            anOut.write(i);
        }
        
        anOut.close();
        
        aListener.start();
        
        Thread.sleep(2000);
    }
    
    
    
    @Test
    public void echo() throws Exception
    {
        URI anAddress = new URI("ws://127.0.0.1:8087/");

        WebSocketListener aService = new WebSocketListener(anAddress);
        WebSocketClient aClient = new WebSocketClient(anAddress);

        try
        {
            final AutoResetEvent aClientOpenedConnectionEvent = new AutoResetEvent(false);
            final AutoResetEvent aResponseReceivedEvent = new AutoResetEvent(false);
            final AutoResetEvent aClientContextReceivedPongEvent = new AutoResetEvent(false);
            final AutoResetEvent aClientContextDisconnectedEvent = new AutoResetEvent(false);

            final byte[][] aReceivedBinMessage = {null};
            final String[] aReceivedTextMessage = {""};

            final boolean[] aClientContextReceivedPong = {false};
            final boolean[] aClientContextReceivedCloseConnection = {false};
            final boolean[] aClientContextDisconnectedAfterClose = {false};

            final IWebSocketClientContext[] aClientContext = {null};

            // Start listening.
            aService.startListening(new IMethod1<IWebSocketClientContext>()
            {
                @Override
                public void invoke(final IWebSocketClientContext clientContext) throws Exception
                {
                    aClientContext[0] = clientContext;

                    clientContext.pongReceived().subscribe(new EventHandler<Object>()
                    {
                        @Override
                        public void onEvent(Object sender, Object e)
                        {
                            aClientContextReceivedPong[0] = true;
                            aClientContextReceivedPongEvent.set();
                        }
                    });

                    clientContext.connectionClosed().subscribe(new EventHandler<Object>()
                    {
                        @Override
                        public void onEvent(Object sender, Object e)
                        {
                            aClientContextReceivedCloseConnection[0] = true;
                            aClientContextDisconnectedAfterClose[0] = !clientContext.isConnected();
                            aClientContextDisconnectedEvent.set();
                        }
                    });

                    WebSocketMessage aWebSocketMessage;
                    while ((aWebSocketMessage = clientContext.receiveMessage()) != null)
                    {
                        if (!aWebSocketMessage.isText())
                        {
                            aReceivedBinMessage[0] = aWebSocketMessage.getWholeMessage();

                            // echo
                            clientContext.sendMessage(aReceivedBinMessage[0]);
                        }
                        else
                        {
                            aReceivedTextMessage[0] = aWebSocketMessage.getWholeTextMessage();

                            // echo
                            clientContext.sendMessage(aReceivedTextMessage[0]);
                        }

                    }
                }
            });
                    

            // Client opens connection.
            final boolean[] aClientReceivedPong = {false};
            final boolean[] aClientOpenedConnection = {false};
            final byte[][] aReceivedBinResponse = {null};
            final String[] aReceivedTextResponse = {""};

            aClient.connectionOpened().subscribe(new EventHandler<Object>()
            {
                @Override
                public void onEvent(Object sender, Object e)
                {
                    aClientOpenedConnection[0] = true;
                    aClientOpenedConnectionEvent.set();
                }
            });
            
            aClient.pongReceived().subscribe(new EventHandler<Object>()
            {
                @Override
                public void onEvent(Object sender, Object e)
                {
                    aClientReceivedPong[0] = true;
                }
            });

            aClient.messageReceived().subscribe(new EventHandler<WebSocketMessage>()
            {
                @Override
                public void onEvent(Object x, WebSocketMessage y)
                {
                    try
                    {
                        if (!y.isText())
                        {
                            aReceivedBinResponse[0] = y.getWholeMessage();
                        }
                        else
                        {
                            aReceivedTextResponse[0] = y.getWholeTextMessage();
                        }
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error("echo test detected an exception when processing received message.", err);
                    }

                    aResponseReceivedEvent.set();
                }
            });

            aClient.openConnection();
            aClientOpenedConnectionEvent.waitOne();

            assertTrue(aClientOpenedConnection[0] == true);

            aClient.sendMessage("He", false);
            aClient.sendMessage("ll", false);
            aClient.sendPing();
            aClient.sendMessage("o", true);

            aResponseReceivedEvent.waitOne();

            assertEquals("Hello", aReceivedTextMessage[0]);
            assertEquals("Hello", aReceivedTextResponse[0]);

            // Client sent ping so it had to receive back the pong.
            assertTrue(aClientReceivedPong[0]);

            aClientContext[0].sendPing();

            aClientContextReceivedPongEvent.waitOne();

            // Service sent ping via the client context - pong had to be received.
            assertTrue(aClientContextReceivedPong[0]);


            byte[] aBinaryMessage = { 1, 2, 3, 100, (byte)200 };
            aClient.sendMessage(new byte[] { 1, 2, 3 }, false);
            aClient.sendMessage(new byte[] { 100, (byte)200 }, true);

            aResponseReceivedEvent.waitOne();

            for (int i = 0; i < aBinaryMessage.length; ++i)
            {
                assertEquals(aBinaryMessage[i], aReceivedBinMessage[0][i]);
                assertEquals(aBinaryMessage[i], aReceivedBinResponse[0][i]);
            }

            aClient.closeConnection();

            aClientContextDisconnectedEvent.waitOne();

            assertTrue(aClientContextReceivedCloseConnection[0]);
            assertTrue(aClientContextDisconnectedAfterClose[0]);
        }
        finally
        {
            aClient.closeConnection();
            aService.stopListening();
        }

    }

    @Test
    public void multipleServicesOneTcpAddress() throws Exception
    {
        URI anAddress1 = new URI("ws://127.0.0.1:8087/MyService1/");
        URI anAddress2 = new URI("ws://127.0.0.1:8087/MyService2/");

        WebSocketListener aService1 = new WebSocketListener(anAddress1);
        WebSocketClient aClient1 = new WebSocketClient(anAddress1);

        WebSocketListener aService2 = new WebSocketListener(anAddress2);
        WebSocketClient aClient2 = new WebSocketClient(anAddress2);

        try
        {
            final AutoResetEvent aResponse1ReceivedEvent = new AutoResetEvent(false);
            final AutoResetEvent aResponse2ReceivedEvent = new AutoResetEvent(false);

            final String[] aReceivedTextMessage1 = {""};
            final String[] aReceivedTextMessage2 = {""};

            // Start listening.
            aService1.startListening(new IMethod1<IWebSocketClientContext>()
                {
                    @Override
                    public void invoke(IWebSocketClientContext clientContext) throws Exception
                    {
                        WebSocketMessage aWebSocketMessage;
                        while ((aWebSocketMessage = clientContext.receiveMessage()) != null)
                        {
                            if (aWebSocketMessage.isText())
                            {
                                String aMessage = aWebSocketMessage.getWholeTextMessage();
    
                                // echo
                                clientContext.sendMessage(aMessage);
                            }
                        }
                    }
                });
            
            aService2.startListening(new IMethod1<IWebSocketClientContext>()
                {
                    @Override
                    public void invoke(IWebSocketClientContext clientContext) throws Exception
                    {
                        WebSocketMessage aWebSocketMessage;
                        while ((aWebSocketMessage = clientContext.receiveMessage()) != null)
                        {
                            if (aWebSocketMessage.isText())
                            {
                                String aMessage = aWebSocketMessage.getWholeTextMessage();

                                // echo
                                clientContext.sendMessage(aMessage);
                            }
                        }
                    }
                });
                    

            // Client opens connection.
            aClient1.messageReceived().subscribe(new EventHandler<WebSocketMessage>()
            {
                @Override
                public void onEvent(Object x, WebSocketMessage y)
                {
                    try
                    {
                        if (y.isText())
                        {
                            aReceivedTextMessage1[0] = y.getWholeTextMessage();
                        }
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error("Exception detected.", err);
                    }

                    aResponse1ReceivedEvent.set();
                }
            });
            
            aClient2.messageReceived().subscribe(new EventHandler<WebSocketMessage>()
            {
                @Override
                public void onEvent(Object x, WebSocketMessage y)
                {
                    try
                    {
                        if (y.isText())
                        {
                            aReceivedTextMessage2[0] = y.getWholeTextMessage();
                        }
                    }
                    catch (Exception err)
                    {
                        EneterTrace.error("Exception detected.", err);
                    }

                    aResponse2ReceivedEvent.set();
                }
            });


            aClient1.openConnection();
            aClient2.openConnection();

            aClient1.sendMessage("He", false);
            aClient1.sendMessage("ll", false);
            aClient2.sendMessage("Hello2");
            aClient1.sendPing();
            aClient1.sendMessage("o1", true);

            aResponse1ReceivedEvent.waitOne();
            aResponse2ReceivedEvent.waitOne();

            assertEquals("Hello1", aReceivedTextMessage1[0]);
            assertEquals("Hello2", aReceivedTextMessage2[0]);
        }
        finally
        {
            aClient1.closeConnection();
            aClient2.closeConnection();
            aService1.stopListening();
            aService2.stopListening();
        }
    }
    
    @Test
    public void query() throws Exception
    {
        URI anAddress = new URI("ws://localhost:8087/MyService/");
        WebSocketListener aService = new WebSocketListener(anAddress);

        // Client will connect with the query.
        URI aClientUri = new URI(anAddress.toString() + "?UserName=user1&Amount=1");
        WebSocketClient aClient = new WebSocketClient(aClientUri);

        try
        {
            final AutoResetEvent aClientConnectedEvent = new AutoResetEvent(false);

            final IWebSocketClientContext[] aClientContext = {null};

            // Start listening.
            aService.startListening(new IMethod1<IWebSocketClientContext>()
            {
                @Override
                public void invoke(IWebSocketClientContext clientContext) throws Exception
                {
                    aClientContext[0] = clientContext;

                    // Indicate the client is connected.
                    aClientConnectedEvent.set();
                }
            });
                    
            aClient.openConnection();

            // Wait until the service accepted and opened the connection.
            //assertTrue(aClientConnectedEvent.waitOne(3000));
            aClientConnectedEvent.waitOne();

            // Check if the query is received.
            assertEquals("UserName=user1&Amount=1", aClientContext[0].getUri().getQuery());
            assertEquals(aClientUri, aClientContext[0].getUri());

        }
        finally
        {
            aClient.closeConnection();
            aService.stopListening();
        }

    }
    
    
    @Test
    public void customHeaderFields() throws Exception
    {
        URI anAddress = new URI("ws://127.0.0.1:8087/MyService/");
        WebSocketListener aService = new WebSocketListener(anAddress);

        // Client will connect with the query.
        WebSocketClient aClient = new WebSocketClient(anAddress);

        try
        {
            final AutoResetEvent aClientConnectedEvent = new AutoResetEvent(false);

            final IWebSocketClientContext[] aClientContext = {null};

            // Start listening.
            aService.startListening(new IMethod1<IWebSocketClientContext>()
            {
                @Override
                public void invoke(IWebSocketClientContext clientContext) throws Exception
                {
                    aClientContext[0] = clientContext;

                    // Indicate the client is connected.
                    aClientConnectedEvent.set();
                }
            });

            aClient.getHeaderFields().put("My-Key1", "hello1");
            aClient.getHeaderFields().put("My-Key2", "hello2");
            aClient.openConnection();

            // Wait until the service accepted and opened the connection.
            //Assert.IsTrue(aClientConnectedEvent.WaitOne(3000));
            aClientConnectedEvent.waitOne();

            // Check if the query is received.
            assertEquals("hello1", aClientContext[0].getHeaderFields().get("My-Key1"));
            assertEquals("hello2", aClientContext[0].getHeaderFields().get("My-Key2"));
        }
        finally
        {
            aClient.closeConnection();
            aService.stopListening();
        }

    }
    
    @Test(expected = IllegalStateException.class)
    public void twoSameListeners() throws Exception
    {
        URI anAddress = new URI("ws://127.0.0.1:8087/MyService/");
        WebSocketListener aService1 = new WebSocketListener(anAddress);

        WebSocketListener aService2 = new WebSocketListener(anAddress);

        try
        {
            // Start the first listener.
            aService1.startListening(new IMethod1<IWebSocketClientContext>()
            {
                @Override
                public void invoke(IWebSocketClientContext t) throws Exception
                {
                }
            });

            // Start the second listener to the same path -> exception is expected.
            aService2.startListening(new IMethod1<IWebSocketClientContext>()
            {
                @Override
                public void invoke(IWebSocketClientContext t) throws Exception
                {
                }
            });
        }
        finally
        {
             aService1.stopListening();
             aService2.stopListening();
        }
    }
    
    @Test
    public void restartListener() throws Exception
    {
        URI anAddress1 = new URI("ws://127.0.0.1:8087/MyService1/");
        WebSocketListener aService1 = new WebSocketListener(anAddress1);

        URI anAddress2 = new URI("ws://127.0.0.1:8087/MyService2/");
        WebSocketListener aService2 = new WebSocketListener(anAddress2);

        try
        {
            IMethod1<IWebSocketClientContext> anEmptyHandler = new IMethod1<IWebSocketClientContext>()
            {
                @Override
                public void invoke(IWebSocketClientContext t) throws Exception
                {
                }
            };
            
            // Start the first listener.
            aService1.startListening(anEmptyHandler);

            // Start the second listener.
            aService2.startListening(anEmptyHandler);

            aService2.stopListening();

            aService2.startListening(anEmptyHandler);
        }
        finally
        {
            aService1.stopListening();
            aService2.stopListening();
        }

    }
    
    @Test
    public void MaxAmountOfClients() throws Exception
    {
        URI anAddress = new URI("ws://127.0.0.1:8081/MyService/");
        IServerSecurityFactory aSecurityFactory = new NoneSecurityServerFactory();
        aSecurityFactory.setMaxAmountOfConnections(2);
        WebSocketListener aService = new WebSocketListener(anAddress, aSecurityFactory);

        // Client will connect with the query.
        WebSocketClient aClient1 = new WebSocketClient(anAddress);
        WebSocketClient aClient2 = new WebSocketClient(anAddress);
        WebSocketClient aClient3 = new WebSocketClient(anAddress);

        try
        {
            // Start listening.
            aService.startListening(clientContext -> { });

            aClient1.openConnection();
            aClient2.openConnection();

            try
            {
                // This opening shall fail with the exception.
                aClient3.openConnection();
            }
            catch (Exception err)
            {
                return;
            }
            
            fail("The third opening did not fail with expected exception.");
        }
        finally
        {
            aClient1.closeConnection();
            aClient2.closeConnection();
            aClient3.closeConnection();
            aService.stopListening();
        }

    }
}

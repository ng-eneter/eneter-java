package eneter.messaging.messagingsystems.httpmessagingsystem;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

public class Test_PolicyServer
{
    @Test
    public void testPolicy() throws Exception
    {
        // Start policy server.
        HttpPolicyServer aPolicyServer = new HttpPolicyServer("http://127.0.0.1:8055/");
        try
        {
            aPolicyServer.startPolicyServer();

            // Request policy xml.
            URL aUrl = new URL("http://127.0.0.1:8055/clientaccesspolicy.xml");
            HttpURLConnection aConnection = (HttpURLConnection)aUrl.openConnection();
            try
            {
                aConnection.setDoOutput(false);
                aConnection.setRequestMethod("GET");
                
                // Fire the message by requesting the response code.
                assertEquals(200, aConnection.getResponseCode());
                
                InputStream aResponseStream = aConnection.getInputStream();
                ByteArrayOutputStream aResponseContentStream = new ByteArrayOutputStream();
                int aSize = 0;
                byte[] aBuffer = new byte[32764];
                while ((aSize = aResponseStream.read(aBuffer)) != -1)
                {
                    aResponseContentStream.write(aBuffer, 0, aSize);
                }
                
                String aPolicyXml = new String(aResponseContentStream.toByteArray(), "UTF-8");
                
                assertEquals(aPolicyServer.getPolicyXml(), aPolicyXml);
            }
            finally
            {
                if (aConnection != null)
                {
                    aConnection.disconnect();
                }
            }
        }
        finally
        {
            aPolicyServer.stopPolicyServer();
        }
    }
}

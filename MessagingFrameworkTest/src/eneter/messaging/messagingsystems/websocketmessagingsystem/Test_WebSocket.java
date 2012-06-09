package eneter.messaging.messagingsystems.websocketmessagingsystem;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class Test_WebSocket
{
    @Test
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
}

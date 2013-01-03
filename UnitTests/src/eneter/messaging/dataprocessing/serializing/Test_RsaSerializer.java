package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.assertEquals;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.junit.Test;

public class Test_RsaSerializer
{
    @Test
    public void SerializeDeserializeWithXml() throws Exception
    {
        Object[] aDataToSerialize = {(int)10, "Hello"};
     
        KeyPairGenerator aKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
        aKeyPairGenerator.initialize(1024);
        KeyPair aKeyPair = aKeyPairGenerator.generateKeyPair();
        RSAPrivateKey aPrivateKey = (RSAPrivateKey)aKeyPair.getPrivate();
        RSAPublicKey aPublicKey = (RSAPublicKey)aKeyPair.getPublic();
        
        RsaSerializer aSerializer = new RsaSerializer(aPublicKey, aPrivateKey);
        Object aSerializedData = aSerializer.serialize(aDataToSerialize, Object[].class);
        
        Object[] aDeserializedData = aSerializer.deserialize(aSerializedData, Object[].class);
        
        assertEquals(2, aDeserializedData.length);
        assertEquals((int)10, aDeserializedData[0]);
        assertEquals("Hello", aDeserializedData[1]);
    }
}

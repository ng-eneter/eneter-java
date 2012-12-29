package eneter.messaging.dataprocessing.serializing;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import org.junit.Test;

public class Test_RsaDigitalSignatureSerializer
{
    @Test
    public void SerializeDeserializeWithXml() throws Exception
    {
        Object[] aDataToSerialize = {(int)10, "Hello"};
        
        // Public certificate
        CertificateFactory aCertificateFactory = CertificateFactory.getInstance("X.509");
        FileInputStream aCertificateStream = new FileInputStream("d:/EneterSigner.cer");
        X509Certificate aCertificate = (X509Certificate) aCertificateFactory.generateCertificate(aCertificateStream);
        
        // Private key
        File aPrivateKeyFile = new File("d:/EneterSigner.pvk");
        BufferedInputStream aBufferedPrivateKey = new BufferedInputStream(new FileInputStream(aPrivateKeyFile));
        byte[] aPrivateKeyBytes = new byte[(int)aPrivateKeyFile.length()];
        aBufferedPrivateKey.read(aPrivateKeyBytes);
        
        KeySpec aKeySpec = new PKCS8EncodedKeySpec(aPrivateKeyBytes);
        RSAPrivateKey aPrivateKey = (RSAPrivateKey)KeyFactory.getInstance("RSA").generatePrivate(aKeySpec);
        
        ISerializer aSerializer = new RsaDigitalSignatureSerializer(aCertificate, aPrivateKey, null, new JavaBinarySerializer());
        Object aSerializedData = aSerializer.serialize(aDataToSerialize, Object[].class);
        
        Object[] aDeserializedData = aSerializer.deserialize(aSerializedData, Object[].class);
        
        assertEquals(2, aDeserializedData.length);
        assertEquals((int)10, aDeserializedData[0]);
        assertEquals("Hello", aDeserializedData[1]);
    }
}

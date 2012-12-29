package eneter.messaging.dataprocessing.serializing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;


import javax.crypto.Cipher;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.IFunction1;

public class RsaDigitalSignatureSerializer implements ISerializer
{
    public RsaDigitalSignatureSerializer(X509Certificate signerCertificate, RSAPrivateKey signerPrivateKey)
    {
        this(signerCertificate, signerPrivateKey, null, new XmlStringSerializer());
    }
    
    
    public RsaDigitalSignatureSerializer(X509Certificate signerPublicCertificate, RSAPrivateKey signerPrivateKey, IFunction1<Boolean, X509Certificate> verifySignerCertificate, ISerializer underlyingSerializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            mySignerPublicCertificate = signerPublicCertificate;
            //myVerifySignerCertificate = (verifySignerCertificate == null) ? VerifySignerCertificate : verifySignerCertificate;
            myUnderlyingSerializer = underlyingSerializer;
            myEncoderDecoder = new EncoderDecoder(underlyingSerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            byte[][] aSignedData = new byte[3][];
            
            // Serialize incoming data using underlying serializer.
            ByteArrayOutputStream aSerializedData = new ByteArrayOutputStream();
            myEncoderDecoder.serialize(aSerializedData, dataToSerialize, clazz);
            aSignedData[0] = aSerializedData.toByteArray();
            
            // Calculate hash from serialized data.
            MessageDigest aSha1 = MessageDigest.getInstance("SHA1");
            byte[] aHash = aSha1.digest(aSignedData[0]);
            
            // Sign the hash.
            // Note: The signature is the hash encrypted with the private key.
            Cipher aCryptoProvider = Cipher.getInstance("RSA");
            aCryptoProvider.init(Cipher.ENCRYPT_MODE, mySignerPrivateKey);
            aSignedData[2] = aCryptoProvider.doFinal(aHash);
            
            // Store the public certificate.
            aSignedData[1] = mySignerPublicCertificate.getPublicKey().getEncoded();
            
            // Serialize data together with the signature.
            Object aSerializedSignedData = myUnderlyingSerializer.serialize(aSignedData, byte[][].class);
            return aSerializedSignedData;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    @Override
    public <T> T deserialize(Object serializedData, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Deserialize data containing the signature.
            byte[][] aSignedData = myUnderlyingSerializer.deserialize(serializedData, byte[][].class);
            
            // Calculate the hash.
            MessageDigest aSha1 = MessageDigest.getInstance("SHA1");
            byte[] aHash = aSha1.digest(aSignedData[0]);
            
            // Verify the certificate.
            CertificateFactory aCertificateFactory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream aCertificateStream = new ByteArrayInputStream(aSignedData[1]);
            X509Certificate aCertificate = (X509Certificate) aCertificateFactory.generateCertificate(aCertificateStream);
            
            
            // Verify the signature.
            Cipher aCryptoProvider = Cipher.getInstance("RSA");
            aCryptoProvider.init(Cipher.DECRYPT_MODE, aCertificate);
            
            // Deserialize data.
            ByteArrayInputStream aDeserializedData = new ByteArrayInputStream(aSignedData[0]);
            return myEncoderDecoder.deserialize(aDeserializedData, clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private ISerializer myUnderlyingSerializer;
    private EncoderDecoder myEncoderDecoder;
    private X509Certificate mySignerPublicCertificate;
    private RSAPrivateKey mySignerPrivateKey;
    
    private String TracedObject = "DigitalSignatureSerializer ";
}

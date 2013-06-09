package eneter.messaging.dataprocessing.serializing;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.RSAPrivateKey;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.IFunction1;

/**
 * Serializer digitaly signing data.
 *
 * Serialization:
 * <ol>
 * <li>Incoming data is serialized by underlying serializer (e.g. XmlStringSerializer)</li>
 * <li>SHA1 hash is calculated from the serialized data.</li>
 * <li>The hash is encrypted with RSA using the private key.</li>
 * <li>The serialized data consists of serialized data, encoded hash (signature) and public certificate of the signer.</li>
 * </ol>
 * Deserialization:
 * <ol>
 * <li>The public certificate is taken from serialized data and verified. (you can provide your own verification)</li>
 * <li>SHA1 hash is calculated from serialized data.</li>
 * <li>Encrypted hash (signature) is decrypted by public key taken from the certificate.</li>
 * <li>If the decrypted hash is same as calculated one the data is ok.</li>
 * <li>Data is deserialized by the underlying serializer and returned.</li>
 * </ol>
 * <pre>
 * {@code
 * String aDataToSerialize = "Hello";
 * 
 * // Public certificate
 * CertificateFactory aCertificateFactory = CertificateFactory.getInstance("X.509");
 * FileInputStream aCertificateStream = new FileInputStream("d:/MySigner.cer");
 * X509Certificate aCertificate = (X509Certificate) aCertificateFactory.generateCertificate(aCertificateStream);
 * 
 * // Private key
 * File aPrivateKeyFile = new File("d:/MySigner.pk8");
 * BufferedInputStream aBufferedPrivateKey = new BufferedInputStream(new FileInputStream(aPrivateKeyFile));
 * byte[] aPrivateKeyBytes = new byte[(int)aPrivateKeyFile.length()];
 * aBufferedPrivateKey.read(aPrivateKeyBytes);
 * KeySpec aKeySpec = new PKCS8EncodedKeySpec(aPrivateKeyBytes);
 * RSAPrivateKey aPrivateKey = (RSAPrivateKey)KeyFactory.getInstance("RSA").generatePrivate(aKeySpec);
 * 
 * // Create serializer
 * ISerializer aSerializer = new RsaDigitalSignatureSerializer(aCertificate, aPrivateKey);
 * 
 * // Serialize
 * Object aSerializedData = aSerializer.serialize(aDataToSerialize, String.class);
 * 
 * // Deserialize
 * String aDeserializedData = aSerializer.deserialize(aSerializedData, String.class);
 * }
 * </pre>
 *
 */
public class RsaDigitalSignatureSerializer implements ISerializer
{
    /**
     * Constructs serializer with default parameters.
     *
     * It uses XmlStringSerializer as the underlying serializer and it uses default X509Certificate.checkValidity() method to verify
     * the public certificate.
     * 
     * if parameters signerCertificate and signerPrivateKey are null then the serializer
     * can be used only for deserialization.
     * 
     * @param signerCertificate public certificate of the signer. This certificate will be attached
     * to serialized data so that the deserializer can verify the signer identity and
     * can check if signed data are not changed. 
     * 
     * @param signerPrivateKey private key that will be used to sign data.
     */
    public RsaDigitalSignatureSerializer(X509Certificate signerCertificate, RSAPrivateKey signerPrivateKey)
    {
        this(signerCertificate, signerPrivateKey, null, new XmlStringSerializer());
    }
    
    /**
     * Constructs serializer with custom parameters.
     * 
     * if parameters signerCertificate and signerPrivateKey are null then the serializer
     * can be used only for deserialization.
     * 
     * @param signerPublicCertificate public certificate of the signer. This certificate will be attached
     * to serialized data so that the deserializer can verify the signer identity and
     * can check if signed data are not changed.
     * 
     * @param signerPrivateKey private key that will be used to sign data.
     * @param verifySignerCertificate Method that will check the signer public certificate before deserializing.
     * If null then default X509Certificate.checkValidity() is used.
     * @param underlyingSerializer underlying serializer used to serialize data. It can
     * be any serializer from this namespace.
     */
    public RsaDigitalSignatureSerializer(X509Certificate signerPublicCertificate, RSAPrivateKey signerPrivateKey, IFunction1<Boolean, X509Certificate> verifySignerCertificate, ISerializer underlyingSerializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (signerPublicCertificate != null && signerPrivateKey == null)
            {
                throw new IllegalArgumentException("The public certificate is present but the parameter signerPrivateKey is null.");
            }
            
            mySignerPublicCertificate = signerPublicCertificate;
            mySignerPrivateKey = signerPrivateKey;
            myVerifySignerCertificate = (verifySignerCertificate == null) ? myVerifySignerCertificate : verifySignerCertificate;
            myUnderlyingSerializer = underlyingSerializer;
            myEncoderDecoder = new EncoderDecoder(underlyingSerializer);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Serializes data.
     */
    @Override
    public <T> Object serialize(T dataToSerialize, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            if (mySignerPublicCertificate == null)
            {
                throw new IllegalStateException(TracedObject() + "failed to serialize data. The signer certificate is null and thus the serializer can be used only for deserialization.");
            }
            
            byte[][] aSignedData = new byte[3][];
            
            // Encode message to the byte sequence.
            ByteArrayOutputStream aSerializedData = new ByteArrayOutputStream();
            myEncoderDecoder.serialize(aSerializedData, dataToSerialize, clazz);
            aSignedData[0] = aSerializedData.toByteArray();
            
            // Sign the message.
            Signature aSigner = Signature.getInstance("SHA1withRSA");
            aSigner.initSign(mySignerPrivateKey, new SecureRandom());
            aSigner.update(aSignedData[0]);
            aSignedData[2] = aSigner.sign();
            
            // Store the public certificate.
            aSignedData[1] = mySignerPublicCertificate.getEncoded();
            
            // Serialize everything with the underlying serializer.
            Object aSerializedSignedData = myUnderlyingSerializer.serialize(aSignedData, byte[][].class);
            return aSerializedSignedData;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    /**
     * Deserializes data.
     */
    @Override
    public <T> T deserialize(Object serializedData, Class<T> clazz)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Deserialize data with underlying serializer.
            byte[][] aSignedData = myUnderlyingSerializer.deserialize(serializedData, byte[][].class);
            
            // Verify the public certificate coming with data.
            CertificateFactory aCertificateFactory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream aCertificateStream = new ByteArrayInputStream(aSignedData[1]);
            X509Certificate aCertificate = (X509Certificate) aCertificateFactory.generateCertificate(aCertificateStream);
            if (!myVerifySignerCertificate.invoke(aCertificate))
            {
                throw new IllegalStateException(TracedObject() + "failed to deserialize data because the verification of signer certificate failed.");
            }

            // Verify the signature.
            Signature aVerifier = Signature.getInstance("SHA1withRSA"); 
            aVerifier.initVerify(aCertificate.getPublicKey());
            aVerifier.update(aSignedData[0]);
            boolean aResult = aVerifier.verify(aSignedData[2]);
            if (aResult == false)
            {
                throw new IllegalStateException(TracedObject() + "failed to deserialize data because the signature verification failed.");
            }
            
            // Decode the byte sequence.
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
    
    private IFunction1<Boolean, X509Certificate> myVerifySignerCertificate = new IFunction1<Boolean, X509Certificate>()
        {
            // If user does not provide his specific method to verify the certificate
            // then this one is the default.
            @Override
            public Boolean invoke(X509Certificate certificate) throws Exception
            {
                certificate.checkValidity();
                
                // Verification passed.
                return true;
            }
        };
    
    private String TracedObject()
    {
        return getClass().getSimpleName() + " ";
    }
}

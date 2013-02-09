package eneter.messaging.dataprocessing.serializing;

import java.security.SecureRandom;
import java.security.interfaces.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import eneter.messaging.diagnostic.EneterTrace;


/**
 * Serializer using RSA.
 *
 *
 * The serialization:
 * <ol>
 * <li>Incoming data is serialized by underlying serializer (e.g. XmlStringSerializer)</li>
 * <li>The random key is generated and used with AES algorythm to encrypt the serialized data.</li>
 * <li>The random key for AES is encrypted by RSA using the public key.</li>
 * <li>The serialized data consits of AES encrypted data and RSA encrypted key for AES.</li>
 * </ol>
 * The deserialization:
 * <ol>
 * <li>The receiver decrypts the AES key by RSA using its private key.</li>
 * <li>Decrypted key is used to decrypt AES encrypted data.</li>
 * <li>Decrypted data is deserialized by underlying serialized (e.g. XmlStringSerializer)</li>
 * <li>The deserialization returns deserialized data.</li>
 * </ol>
 * <br/>
 * <pre>
 * {@code
 * String aDataToSerialize = "Hello";
 * 
 * KeyPairGenerator aKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
 * aKeyPairGenerator.initialize(1024);
 * KeyPair aKeyPair = aKeyPairGenerator.generateKeyPair();
 * RSAPrivateKey aPrivateKey = (RSAPrivateKey)aKeyPair.getPrivate();
 * RSAPublicKey aPublicKey = (RSAPublicKey)aKeyPair.getPublic();
 * 
 * RsaSerializer aSerializer = new RsaSerializer(aPublicKey, aPrivateKey);
 * 
 * Object aSerializedData = aSerializer.serialize(aDataToSerialize, String.class);
 * 
 * String aDeserializedData = aSerializer.deserialize(aSerializedData, String.class);
 * }
 * </pre>
 *
 */
public class RsaSerializer implements ISerializer
{
    /**
     * Constructs the RSA serializer with default paraneters.
     * 
     * It uses XmlStringSerializer and it will generate 128 bit key for the AES algorythm.
     * 
     * @param publicKey public key used for serialization. It can be null if the serializer will be used only for deserialization.
     * @param privateKey private key used for deserialization. It can be null if the serializer will be used only for serialization.
     */
    public RsaSerializer(RSAPublicKey publicKey, RSAPrivateKey privateKey)
    {
        this(publicKey, privateKey, 128, new XmlStringSerializer());
    }
    

    /**
     * Constructs the RSA serializer with custom parameters.
     * 
     * 
     * 
     * @param publicKey publicKey public key used for serialization. It can be null if the serializer will be used only for deserialization.
     * @param privateKey private key used for deserialization. It can be null if the serializer will be used only for serialization.
     * @param aesBitSize size of the random key generated for the AES encryption, 128, 256, ...
     * @param underlyingSerializer underlying serializer used to serialize/deserialize data e.g. XmlStringSerializer
     */
    public RsaSerializer(RSAPublicKey publicKey, RSAPrivateKey privateKey, int aesBitSize, ISerializer underlyingSerializer)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myPublicKey = publicKey;
            myPrivateKey = privateKey;
            myAesBitSize = aesBitSize;
            myUnderlyingSerializer = underlyingSerializer;
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
    public <T> Object serialize(T dataToSerialize, Class<T> clazz) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Generate key and initialization vector for AES.
            KeyGenerator aKeyGenerator = KeyGenerator.getInstance("AES");
            aKeyGenerator.init(myAesBitSize);
            SecretKey aKey = aKeyGenerator.generateKey();
            
            SecureRandom aSecureRandom = new SecureRandom();
            byte[] anInitializationVector = aSecureRandom.generateSeed(16);
            IvParameterSpec anIv = new IvParameterSpec(anInitializationVector);
            
            byte[][] aData = new byte[3][];
            
            // Serialize data 
            AesSerializer anAesSerializer = new AesSerializer(aKey, anIv, myUnderlyingSerializer);
            aData[2] = (byte[])anAesSerializer.serialize(dataToSerialize, clazz);
            
            // Encrypt the random key with RSA using the public key.
            // Note: Only guy having the private key can decrypt it.
            Cipher aCryptoProvider = Cipher.getInstance("RSA");
            aCryptoProvider.init(Cipher.ENCRYPT_MODE, myPublicKey);
            aData[0] = aCryptoProvider.doFinal(aKey.getEncoded());
            aData[1] = aCryptoProvider.doFinal(anInitializationVector);
            
            Object aSerializedData = myUnderlyingSerializer.serialize(aData, byte[][].class);
            return aSerializedData;
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
    public <T> T deserialize(Object serializedData, Class<T> clazz) throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            // Deserialize data
            byte[][] aData = myUnderlyingSerializer.deserialize(serializedData, byte[][].class);
            
            // Use the private key to decrypt the key and iv for the AES.
            Cipher aCryptoProvider = Cipher.getInstance("RSA");
            aCryptoProvider.init(Cipher.DECRYPT_MODE, myPrivateKey);
            byte[] aKeyBytes = aCryptoProvider.doFinal(aData[0]);
            byte[] anIvBytes = aCryptoProvider.doFinal(aData[1]);
            
            // Decrypt data content which its encrypted with AES.
            SecretKeySpec aKey = new SecretKeySpec(aKeyBytes, "AES");
            IvParameterSpec anIv = new IvParameterSpec(anIvBytes);
            
            AesSerializer anAes = new AesSerializer(aKey, anIv, myUnderlyingSerializer);
            T aDeserializedData = anAes.deserialize(aData[2], clazz);
            
            return aDeserializedData;
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    
    private ISerializer myUnderlyingSerializer;
    private int myAesBitSize;
    private RSAPrivateKey myPrivateKey;
    private RSAPublicKey myPublicKey;
}

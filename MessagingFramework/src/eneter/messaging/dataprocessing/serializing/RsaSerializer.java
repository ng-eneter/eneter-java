package eneter.messaging.dataprocessing.serializing;

import java.security.SecureRandom;
import java.security.interfaces.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import eneter.messaging.diagnostic.EneterTrace;

public class RsaSerializer implements ISerializer
{
    public RsaSerializer(RSAPublicKey publicKey, RSAPrivateKey privateKey)
    {
        this(publicKey, privateKey, 128, new XmlStringSerializer());
    }
    
    public RsaSerializer(RSAPublicKey publicKey, RSAPrivateKey privateKey, int aesBitSize)
    {
        this(publicKey, privateKey, aesBitSize, new XmlStringSerializer());
    }

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
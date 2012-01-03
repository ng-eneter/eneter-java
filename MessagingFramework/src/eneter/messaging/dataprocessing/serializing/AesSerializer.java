package eneter.messaging.dataprocessing.serializing;

import java.io.*;
import java.security.Key;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.security.cryptography.Rfc2898DeriveBytes;

public class AesSerializer implements ISerializer
{
    public AesSerializer(String password) 
            throws Exception
    {
        this(password, new byte[] { 1, 3, 5, 8, 15, (byte)254, 9, (byte)189, 43, (byte)129 }, new XmlStringSerializer());
    }
    
    public AesSerializer(String password, ISerializer underlyingSerializer) 
            throws Exception
    {
        this(password, new byte[] { 1, 3, 5, 8, 15, (byte)254, 9, (byte)189, 43, (byte)129 }, underlyingSerializer);
    }
    
    public AesSerializer(String password, byte[] salt) 
            throws Exception
    {
        this(password, salt, new XmlStringSerializer());
    }
    
    public AesSerializer(String password, byte[] salt, ISerializer underlyingSerializer)
            throws Exception
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            Rfc2898DeriveBytes aKeyGenerator = new Rfc2898DeriveBytes(password, salt);
            myKey = new SecretKeySpec(aKeyGenerator.getBytes(128 / 8), "AES");
            myInitializeVector = new IvParameterSpec(aKeyGenerator.getBytes(16));
            
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
            // Create stream where encrypted data will be stored.
            ByteArrayOutputStream anEncryptedData = new ByteArrayOutputStream();
            
            // Create stream encrypting data.
            Cipher aCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aCipher.init(Cipher.ENCRYPT_MODE, myKey, myInitializeVector);
            CipherOutputStream anEncryptor = new CipherOutputStream(anEncryptedData, aCipher);

            // Serialize and encrypt the data.
            myEncoderDecoder.serialize(anEncryptor, dataToSerialize, clazz);

            anEncryptor.close();
            return anEncryptedData.toByteArray();
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
            byte[] anEncryptedData = (byte[])serializedData;

            // Put encrypted data to the stream.
            ByteArrayInputStream anEncryptedDataStream = new ByteArrayInputStream(anEncryptedData);
            
            // Create stream decrypting the data.
            Cipher aCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aCipher.init(Cipher.DECRYPT_MODE, myKey, myInitializeVector);
            CipherInputStream aDecryptor = new CipherInputStream(anEncryptedDataStream, aCipher);

            // Deserialize the encrypted data.
            return myEncoderDecoder.deserialize(aDecryptor, clazz);
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    

    private EncoderDecoder myEncoderDecoder;
    
    private Key myKey;
    private IvParameterSpec myInitializeVector;
}

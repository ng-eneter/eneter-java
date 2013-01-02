package eneter.messaging.dataprocessing.serializing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;


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
            aSignedData[1] = mySignerPublicCertificate.getEncoded();
            
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
            byte[] aCalculatedHash = aSha1.digest(aSignedData[0]);
            
            // Verify the certificate.
            CertificateFactory aCertificateFactory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream aCertificateStream = new ByteArrayInputStream(aSignedData[1]);
            X509Certificate aCertificate = (X509Certificate) aCertificateFactory.generateCertificate(aCertificateStream);
            if (!myVerifySignerCertificate.invoke(aCertificate))
            {
                throw new IllegalStateException(TracedObject + "failed to deserialize data because the verification of signer certificate failed.");
            }

            // Decrypt the signature and verify it.
            Cipher aCryptoProvider = Cipher.getInstance("RSA");
            aCryptoProvider.init(Cipher.DECRYPT_MODE, aCertificate);
            byte[] aDecryptedHash = aCryptoProvider.doFinal(aSignedData[2]);
            if (aDecryptedHash.length != aCalculatedHash.length)
            {
                throw new IllegalStateException(TracedObject + "failed to deserialize data because the signature verification failed.");
            }
            for (int i = 0; i < aDecryptedHash.length; ++i)
            {
                if (aDecryptedHash[i] != aCalculatedHash[i])
                {
                    throw new IllegalStateException(TracedObject + "failed to deserialize data because the signature verification failed.");
                }
            }
            
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
    private IFunction1<Boolean, X509Certificate> myVerifySignerCertificate = new IFunction1<Boolean, X509Certificate>()
        {
            // If user does not provide his specific method to verify the certificate
            // then this one is the default.
            @Override
            public Boolean invoke(X509Certificate certificate) throws Exception
            {
                certificate.checkValidity();
                
                //ArrayList<X509Certificate> aCertificates = new ArrayList<X509Certificate>();
                //aCertificates.add(certificate);
                
                //CertificateFactory aCertificateFactory = CertificateFactory.getInstance("X.509");
                
                //// Get chain of certificates.
                //CertPath aCertificatePath = aCertificateFactory.generateCertPath(aCertificates);
                
                //KeyStore aKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                //aKeyStore.load(new FileInputStream(new File(System.getProperty("user.home"), ".keystore")), null);
                
                //PKIXParameters aPkiParameters = new PKIXParameters(aKeyStore);
                
                //CertPathValidator aCertificatePathValidator = CertPathValidator.getInstance("PKIX");
                
                //try
                //{
                //    PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) aCertificatePathValidator.validate(aCertificatePath, aPkiParameters);
                //}
                //catch (Exception err)
                //{
                //    // Verification of the certificate failed.
                //    return false;
                //}
                
                // Verification passed.
                return true;
            }
        };
    
    private String TracedObject = "DigitalSignatureSerializer ";
}

package eneter.messaging.dataprocessing.serializing;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.RSAPrivateKey;

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
                throw new IllegalStateException(TracedObject + "failed to deserialize data because the verification of signer certificate failed.");
            }

            // Verify the signature.
            Signature aVerifier = Signature.getInstance("SHA1withRSA"); 
            aVerifier.initVerify(aCertificate.getPublicKey());
            aVerifier.update(aSignedData[0]);
            boolean aResult = aVerifier.verify(aSignedData[2]);
            if (aResult == false)
            {
                throw new IllegalStateException(TracedObject + "failed to deserialize data because the signature verification failed.");
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

package com.demo.upimesh.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.core.SdkBytes;

import java.util.Base64;

/**
 * AWS KMS (Key Management Service) Integration
 * Provides encrypted/decrypted operations for sensitive keys
 * 
 * Production: All private keys are stored in AWS KMS, never in application memory
 */
@Service
@ConditionalOnProperty(name = "aws.kms.enabled", havingValue = "true")
public class KmsKeyService {

    private static final Logger log = LoggerFactory.getLogger(KmsKeyService.class);

    @Autowired(required = false)
    private KmsClient kmsClient;
    
    @Value("${aws.kms.key-id:}")
    private String kmsKeyId;
    
    @Value("${aws.kms.region:us-east-1}")
    private String region;

    /**
     * Decrypt a ciphertext blob using AWS KMS
     * The ciphertext is typically the RSA private key or AES key wrapped by KMS
     * 
     * @param encryptedKeyBlob Base64-encoded encrypted key from KMS
     * @return Decrypted plaintext key bytes
     * @throws RuntimeException if KMS call fails
     */
    public byte[] decryptWithKms(String encryptedKeyBlob) {
        if (kmsClient == null) {
            throw new IllegalStateException("KMS client not initialized. Check AWS credentials and configuration.");
        }
        
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedKeyBlob);
            
            DecryptRequest decryptRequest = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(encryptedData))
                .keyId(kmsKeyId)
                .build();
            
            DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);
            byte[] plaintext = decryptResponse.plaintext().asByteArray();
            
            log.debug("Successfully decrypted key using KMS");
            return plaintext;
            
        } catch (Exception e) {
            log.error("KMS decryption failed: {}", e.getMessage());
            throw new RuntimeException("KMS key decryption failed", e);
        }
    }

    /**
     * Get the KMS Key ID being used
     */
    public String getKmsKeyId() {
        return kmsKeyId;
    }

    /**
     * Verify if KMS is properly configured
     */
    public boolean isKmsConfigured() {
        return kmsClient != null && kmsKeyId != null && !kmsKeyId.isEmpty();
    }
}

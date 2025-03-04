package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.utility.AESUtility;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class EncryptionService {

    private final SecretKey secretKey;

    // AES 키 생성
    public EncryptionService() throws Exception {
        this.secretKey = AESUtility.generateKey();
    }

    public String encryptPrimaryKey(Long primaryKey) throws Exception {
        return AESUtility.encrypt(String.valueOf(primaryKey), secretKey);
    }

    public Long decryptPrimaryKey(String encryptedKey) throws Exception {
        String decryptedKey = AESUtility.decrypt(encryptedKey, secretKey);
        return Long.parseLong(decryptedKey);
    }

    // AES 키를 Base64로 인코딩하여 외부에 전달
    public String getEncodedSecretKey() {
        return AESUtility.encodeKey(secretKey);
    }
}

package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService;
import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService.CredentialPublicKey;

@RestController
@RequestMapping("/security/credential-encryption")
public class CredentialEncryptionController {

    private final CredentialEncryptionService credentialEncryptionService;

    public CredentialEncryptionController(CredentialEncryptionService credentialEncryptionService) {
        this.credentialEncryptionService = credentialEncryptionService;
    }

    @GetMapping("/public-key")
    public ResponseEntity<CredentialPublicKey> publicKey() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(credentialEncryptionService.getPublicKey());
    }
}

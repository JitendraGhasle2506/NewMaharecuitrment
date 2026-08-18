package com.maharecruitment.gov.in.common.security;

import java.util.Map;

/**
 * Decrypts a fresh, purpose-bound collection of sensitive browser fields.
 *
 * <p>The implementation lives at the web boundary, which owns the RSA key pair.
 * Domain services depend only on this contract and receive plaintext only for the
 * short period needed to validate and persist the submitted values.</p>
 */
public interface SensitivePayloadDecryptor {

    Map<String, String> decryptSensitivePayloads(
            Map<String, String> encryptedValues,
            String submittedKeyId,
            long timestamp,
            String nonce,
            String purpose);
}

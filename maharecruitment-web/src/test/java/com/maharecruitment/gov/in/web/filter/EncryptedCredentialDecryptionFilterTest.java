package com.maharecruitment.gov.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService;
import com.maharecruitment.gov.in.web.security.headers.SecurityHeaderPolicy;

import jakarta.servlet.http.HttpServletRequest;

class EncryptedCredentialDecryptionFilterTest {

    private static final OAEPParameterSpec OAEP_SHA_256 = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT);

    private final CredentialEncryptionService credentialEncryptionService = new CredentialEncryptionService();
    private final EncryptedCredentialDecryptionFilter filter =
            new EncryptedCredentialDecryptionFilter(credentialEncryptionService);

    @Test
    void encryptedLoginPasswordIsDecryptedBeforeAuthentication() throws Exception {
        String encryptedPassword = encryptWithPublishedPublicKey("Password@123");
        MockHttpServletRequest request = post("/doLogin");
        request.addParameter("username", "hr@mahait.org");
        request.addParameter("password", encryptedPassword);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> passwordSeenByAuthentication = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                passwordSeenByAuthentication.set(((HttpServletRequest) servletRequest).getParameter("password")));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(passwordSeenByAuthentication).hasValue("Password@123");
    }

    @Test
    void plaintextLoginPasswordIsRejectedBeforeAuthentication() throws Exception {
        MockHttpServletRequest request = post("/doLogin");
        request.addParameter("username", "hr@mahait.org");
        request.addParameter("password", "Password@123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).isEqualTo("Encrypted password is required for login.");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Content-Security-Policy"))
                .isEqualTo(SecurityHeaderPolicy.CONTENT_SECURITY_POLICY);
        assertThat(response.getHeader("Strict-Transport-Security"))
                .contains("max-age=31536000")
                .contains("includeSubDomains");
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void invalidEncryptedLoginPasswordIsRejectedBeforeAuthentication() throws Exception {
        MockHttpServletRequest request = post("/doLogin");
        request.addParameter("username", "hr@mahait.org");
        request.addParameter("password", CredentialEncryptionService.ENCRYPTED_PREFIX + "invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString())
                .isEqualTo("Unable to process encrypted credentials. Please refresh and try again.");
        assertThat(chainInvoked).isFalse();
    }

    private MockHttpServletRequest post(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", requestUri);
        request.setSecure(true);
        return request;
    }

    private String encryptWithPublishedPublicKey(String credential) throws Exception {
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(
                        Base64.getDecoder().decode(credentialEncryptionService.getPublicKey().publicKey())));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA_256);
        byte[] encrypted = cipher.doFinal(credential.getBytes(StandardCharsets.UTF_8));
        return CredentialEncryptionService.ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(encrypted);
    }
}

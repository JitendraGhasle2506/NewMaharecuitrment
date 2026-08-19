package com.maharecruitment.gov.in.web.service.master;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.maharecruitment.gov.in.master.dto.AgencyMasterRequest;
import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.master.service.AgencyMasterService;
import com.maharecruitment.gov.in.web.dto.master.AgencyMasterForm;
import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;
import com.maharecruitment.gov.in.web.service.master.impl.AgencyMasterPageServiceImpl;
import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;

class AgencyMasterPageServiceSecurityTest {

    private static final String CERTIFICATE_NUMBER = "C".repeat(100);
    private static final OAEPParameterSpec OAEP_SHA_256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    @Test
    void decryptsAgencyIdentityOnlyInsideServiceBeforePersistence() throws Exception {
        CredentialEncryptionService transport = new CredentialEncryptionService();
        AgencyMasterService masterService = mock(AgencyMasterService.class);
        when(masterService.create(any())).thenReturn(new AgencyMasterResponse());
        AgencyMasterPageServiceImpl service = service(masterService, transport);
        AgencyMasterForm form = encryptedForm(transport, "agency-create-sensitive-nonce");

        service.create(form);

        ArgumentCaptor<AgencyMasterRequest> request = ArgumentCaptor.forClass(AgencyMasterRequest.class);
        verify(masterService).create(request.capture());
        assertThat(request.getValue().getPanNumber()).isEqualTo("ABCDE2546F");
        assertThat(request.getValue().getGstNumber()).isEqualTo("27ABCDE1234F1Z5");
        assertThat(request.getValue().getBankAccountNumber()).isEqualTo("123456789012");
        assertThat(request.getValue().getIfscCode()).isEqualTo("TEST0001234");
        assertThat(request.getValue().getCertificateNumber()).isEqualTo(CERTIFICATE_NUMBER);
        assertThat(form.getPanNumberEncrypted()).isNull();
        assertThat(form.getGstNumberEncrypted()).isNull();
        assertThat(form.getBankAccountNumberEncrypted()).isNull();
        assertThat(form.getIfscCodeEncrypted()).isNull();
        assertThat(form.getCertificateNumberEncrypted()).isNull();
    }

    @Test
    void rejectsPlaintextOnlyAgencyIdentitySubmission() {
        AgencyMasterService masterService = mock(AgencyMasterService.class);
        AgencyMasterPageServiceImpl service = service(masterService, new CredentialEncryptionService());
        AgencyMasterForm form = baseForm();
        form.setPanNumber("ABCDE2546F");
        form.setGstNumber("27ABCDE1234F1Z5");
        form.setBankAccountNumber("123456789012");
        form.setIfscCode("TEST0001234");
        form.setCertificateNumber("CERT-2026-001");

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to process the submitted agency identity information.");
        verify(masterService, never()).create(any());
    }

    @Test
    void updateKeepsDatabaseValuesWithoutReturningThemToBrowser() {
        AgencyMasterService masterService = mock(AgencyMasterService.class);
        when(masterService.update(eq(42L), any())).thenReturn(new AgencyMasterResponse());
        AgencyMasterPageServiceImpl service = service(masterService, new CredentialEncryptionService());

        service.update(42L, baseForm());

        ArgumentCaptor<AgencyMasterRequest> request = ArgumentCaptor.forClass(AgencyMasterRequest.class);
        verify(masterService).update(eq(42L), request.capture());
        verify(masterService, never()).getById(any());
        assertThat(request.getValue().getPanNumber()).isNull();
        assertThat(request.getValue().getGstNumber()).isNull();
        assertThat(request.getValue().getBankAccountNumber()).isNull();
        assertThat(request.getValue().getIfscCode()).isNull();
        assertThat(request.getValue().getCertificateNumber()).isNull();
    }

    @Test
    void emailsNewAgencyCredentialsAndRemovesPasswordFromReturnedResponse() throws Exception {
        CredentialEncryptionService transport = new CredentialEncryptionService();
        AgencyMasterService masterService = mock(AgencyMasterService.class);
        AccountNotificationService notifications = mock(AccountNotificationService.class);
        AgencyMasterResponse response = new AgencyMasterResponse();
        response.setAgencyUserCreated(true);
        response.setProvisionedUserEmail("agency@example.test");
        response.setContactPersonName("Agency Contact");
        response.setContactPersonMobileNo("9876543210");
        response.setTemporaryPassword("TemporaryPassword1!");
        when(masterService.create(any())).thenReturn(response);
        AgencyMasterPageServiceImpl service = service(masterService, notifications, transport);

        AgencyMasterResponse returned = service.create(encryptedForm(transport, "agency-email-sensitive-nonce"));

        verify(notifications).sendAgencyCredentials(
                "agency@example.test",
                "9876543210",
                "Agency Contact",
                "agency@example.test",
                "TemporaryPassword1!");
        assertThat(returned.getTemporaryPassword()).isNull();
    }

    private AgencyMasterPageServiceImpl service(
            AgencyMasterService masterService,
            CredentialEncryptionService transport) {
        return service(masterService, mock(AccountNotificationService.class), transport);
    }

    private AgencyMasterPageServiceImpl service(
            AgencyMasterService masterService,
            AccountNotificationService notifications,
            CredentialEncryptionService transport) {
        return new AgencyMasterPageServiceImpl(
                masterService,
                mock(FileStorageService.class),
                notifications,
                mock(AgencyAccessService.class),
                transport);
    }

    private AgencyMasterForm encryptedForm(
            CredentialEncryptionService transport,
            String nonce) throws Exception {
        AgencyMasterForm form = baseForm();
        form.setEncryptionKeyId(transport.getPublicKey().keyId());
        form.setTimestamp(System.currentTimeMillis());
        form.setNonce(nonce);
        form.setPanNumberEncrypted(encryptSensitive(transport, form, "panNumber", "ABCDE2546F"));
        form.setGstNumberEncrypted(encryptSensitive(transport, form, "gstNumber", "27ABCDE1234F1Z5"));
        form.setBankAccountNumberEncrypted(encryptSensitive(
                transport, form, "bankAccountNumber", "123456789012"));
        form.setIfscCodeEncrypted(encryptSensitive(transport, form, "ifscCode", "TEST0001234"));
        form.setCertificateNumberEncrypted(encryptSensitive(
                transport, form, "certificateNumber", CERTIFICATE_NUMBER));
        return form;
    }

    private AgencyMasterForm baseForm() {
        AgencyMasterForm form = new AgencyMasterForm();
        form.setExistingPanCopyPath("agency-master/pan/pan.pdf");
        form.setExistingCertificateDocumentPath("agency-master/certificate/certificate.pdf");
        form.setExistingGstDocumentPath("agency-master/gst/gst.pdf");
        form.setExistingCancelledChequePath("agency-master/cancelled-cheque/cheque.pdf");
        return form;
    }

    private String encryptSensitive(
            CredentialEncryptionService service,
            AgencyMasterForm form,
            String field,
            String value) throws Exception {
        String envelope = String.join("\n",
                CredentialEncryptionService.SENSITIVE_PAYLOAD_PREFIX,
                form.getEncryptionKeyId(),
                Long.toString(form.getTimestamp()),
                form.getNonce(),
                "AGENCY_MASTER",
                field,
                value);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(
                Base64.getDecoder().decode(service.getPublicKey().publicKey())));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA_256);
        return CredentialEncryptionService.ENCRYPTED_PREFIX
                + Base64.getEncoder().encodeToString(cipher.doFinal(envelope.getBytes(StandardCharsets.UTF_8)));
    }
}

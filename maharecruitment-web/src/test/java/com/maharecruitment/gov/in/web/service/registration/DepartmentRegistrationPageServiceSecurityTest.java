package com.maharecruitment.gov.in.web.service.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.mock.web.MockMultipartFile;

import com.maharecruitment.gov.in.auth.dto.DepartmentRegistrationRequest;
import com.maharecruitment.gov.in.auth.dto.DepartmentUserProvisioningResult;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.service.DepartmentRegistrationService;
import com.maharecruitment.gov.in.auth.service.DepartmentUserProvisioningService;
import com.maharecruitment.gov.in.master.dto.DepartmentResponse;
import com.maharecruitment.gov.in.master.service.DepartmentMstService;
import com.maharecruitment.gov.in.master.service.SubDepartmentService;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.registration.DepartmentRegistrationForm;
import com.maharecruitment.gov.in.web.service.registration.impl.DepartmentRegistrationPageServiceImpl;
import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;

class DepartmentRegistrationPageServiceSecurityTest {

    private static final OAEPParameterSpec OAEP_SHA_256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    @Test
    void decryptsTransportValidatesAndPassesNormalizedPlaintextToPersistenceService() throws Exception {
        CredentialEncryptionService transport = new CredentialEncryptionService();
        DepartmentRegistrationService registrationService = mock(DepartmentRegistrationService.class);
        DepartmentRegistrationPageServiceImpl service = service(transport, registrationService);
        DepartmentRegistrationForm form = validForm(transport, "unique-registration-nonce");

        DepartmentRegistrationEntity saved = new DepartmentRegistrationEntity();
        saved.setDepartmentRegistrationId(101L);
        when(registrationService.registerDepartment(any())).thenReturn(saved);

        service.register(form);

        ArgumentCaptor<DepartmentRegistrationRequest> captor =
                ArgumentCaptor.forClass(DepartmentRegistrationRequest.class);
        verify(registrationService).registerDepartment(captor.capture());
        DepartmentRegistrationRequest request = captor.getValue();
        assertThat(request.getPanNo()).isEqualTo("ABCDE2546F");
        assertThat(request.getGstNo()).isEqualTo("27ABCDE1234F1Z5");
        assertThat(form.getPanNumberEncrypted()).isNull();
        assertThat(form.getGstNumberEncrypted()).isNull();
    }

    @Test
    void invalidCiphertextReturnsOnlyGenericIdentityFailure() throws Exception {
        CredentialEncryptionService transport = new CredentialEncryptionService();
        DepartmentRegistrationService registrationService = mock(DepartmentRegistrationService.class);
        FileStorageService files = mock(FileStorageService.class);
        when(files.isManagedPath(anyString())).thenReturn(true);
        DepartmentRegistrationPageServiceImpl service = service(transport, registrationService, files);
        DepartmentRegistrationForm form = validForm(transport, "another-unique-request-nonce");
        form.setPanFile(new MockMultipartFile("panFile", "pan.pdf", "application/pdf", "test".getBytes()));
        form.setPanNumberEncrypted(CredentialEncryptionService.ENCRYPTED_PREFIX + "invalid");

        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to process the submitted identity information.");
        assertThat(form.getPanNumberEncrypted()).isNull();
        verify(files, never()).store(any(), anyString());
    }

    @Test
    void invalidDecryptedFormatReturnsOnlyGenericIdentityFailure() throws Exception {
        CredentialEncryptionService transport = new CredentialEncryptionService();
        DepartmentRegistrationService registrationService = mock(DepartmentRegistrationService.class);
        DepartmentRegistrationPageServiceImpl service = service(transport, registrationService);
        DepartmentRegistrationForm form = validForm(transport, "format-validation-request-nonce");
        form.setPanNumberEncrypted(encryptSensitive(transport, form, "panNumber", "INVALID"));

        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to process the submitted identity information.");
        verify(registrationService, never()).registerDepartment(any());
    }

    @SuppressWarnings("unchecked")
    private DepartmentRegistrationPageServiceImpl service(
            CredentialEncryptionService transport,
            DepartmentRegistrationService registrationService) {
        FileStorageService files = mock(FileStorageService.class);
        when(files.isManagedPath(anyString())).thenReturn(true);
        return service(transport, registrationService, files);
    }

    @SuppressWarnings("unchecked")
    private DepartmentRegistrationPageServiceImpl service(
            CredentialEncryptionService transport,
            DepartmentRegistrationService registrationService,
            FileStorageService files) {
        DepartmentMstService departments = mock(DepartmentMstService.class);
        when(departments.getById(1L)).thenReturn(DepartmentResponse.builder()
                .departmentId(1L).departmentName("Department").build());
        when(files.store(any(), anyString())).thenAnswer(invocation -> {
            org.springframework.web.multipart.MultipartFile file = invocation.getArgument(0);
            String category = invocation.getArgument(1);
            return new FileUploadResult(
                    file.getOriginalFilename(),
                    file.getOriginalFilename(),
                    category + "/" + file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize());
        });
        DepartmentUserProvisioningService provisioning = mock(DepartmentUserProvisioningService.class);
        when(provisioning.createDepartmentUser(any())).thenReturn(DepartmentUserProvisioningResult.builder()
                .userId(9L).email("generated@example.test").temporaryPassword("temporary").build());
        return new DepartmentRegistrationPageServiceImpl(
                departments,
                mock(SubDepartmentService.class),
                registrationService,
                provisioning,
                files,
                mock(AccountNotificationService.class),
                transport);
    }

    private DepartmentRegistrationForm validForm(CredentialEncryptionService transport, String nonce) throws Exception {
        DepartmentRegistrationForm form = new DepartmentRegistrationForm();
        form.setDepartmentId(1L);
        form.setAddress("Official office address");
        form.setPrimaryContactName("Primary Officer");
        form.setPrimaryDesignation("Officer");
        form.setPrimaryMobile("9876543210");
        form.setPrimaryEmail("primary@example.test");
        form.setSecondaryContactName("Secondary Officer");
        form.setSecondaryDesignation("Officer");
        form.setSecondaryMobile("9876543211");
        form.setSecondaryEmail("secondary@example.test");
        form.setBillDepartmentName("Billing Department");
        form.setTanNo("ABCD12345E");
        form.setBillAddress("Official billing address");
        form.setGstFile(pdf("gstFile", "gst.pdf"));
        form.setPanFile(pdf("panFile", "pan.pdf"));
        form.setTanFile(pdf("tanFile", "tan.pdf"));
        form.setIsTermsConditionAccepted(true);
        form.setEncryptionKeyId(transport.getPublicKey().keyId());
        form.setTimestamp(System.currentTimeMillis());
        form.setNonce(nonce);
        form.setPanNumberEncrypted(encryptSensitive(transport, form, "panNumber", "ABCDE2546F"));
        form.setGstNumberEncrypted(encryptSensitive(transport, form, "gstNumber", "27ABCDE1234F1Z5"));
        return form;
    }

    private MockMultipartFile pdf(String field, String name) {
        return new MockMultipartFile(field, name, "application/pdf", "%PDF-test".getBytes(StandardCharsets.UTF_8));
    }

    private String encryptSensitive(CredentialEncryptionService service, DepartmentRegistrationForm form,
            String field, String value) throws Exception {
        return encrypt(service, String.join("\n",
                CredentialEncryptionService.SENSITIVE_PAYLOAD_PREFIX,
                form.getEncryptionKeyId(),
                Long.toString(form.getTimestamp()),
                form.getNonce(),
                "DEPARTMENT_REGISTRATION",
                field,
                value));
    }

    private String encrypt(CredentialEncryptionService service, String value) throws Exception {
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(
                Base64.getDecoder().decode(service.getPublicKey().publicKey())));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA_256);
        return CredentialEncryptionService.ENCRYPTED_PREFIX
                + Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}

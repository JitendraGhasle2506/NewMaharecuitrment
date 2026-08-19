package com.maharecruitment.gov.in.common.mahaitprofile.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileRequest;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfile;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfileAuditAction;
import com.maharecruitment.gov.in.common.mahaitprofile.repository.MahaItProfileAuditLogRepository;
import com.maharecruitment.gov.in.common.mahaitprofile.repository.MahaItProfileRepository;
import com.maharecruitment.gov.in.common.mahaitprofile.service.MahaItProfileAuditService;
import com.maharecruitment.gov.in.common.security.SensitivePayloadDecryptor;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

class MahaItProfileServiceImplTest {

    private final MahaItProfileRepository profileRepository = mock(MahaItProfileRepository.class);
    private final MahaItProfileAuditLogRepository auditLogRepository =
            mock(MahaItProfileAuditLogRepository.class);
    private final MahaItProfileAuditService auditService = mock(MahaItProfileAuditService.class);
    private final CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
    private final SensitivePayloadDecryptor decryptor = mock(SensitivePayloadDecryptor.class);

    private MahaItProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MahaItProfileServiceImpl(
                profileRepository,
                auditLogRepository,
                auditService,
                currentActorProvider,
                decryptor);
    }

    @Test
    void createDecryptsIdentifiersImmediatelyBeforeSavingPlaintext() {
        MahaItProfileRequest request = validRequest();
        request.setCinNumber("ATTACKER-PLAINTEXT");
        request.setPanNumber("ATTACKER-PLAINTEXT");
        request.setGstNumber("ATTACKER-PLAINTEXT");
        request.setAccountNumber("000000");
        request.setIfscCode("PLAIN0000000");

        when(decryptor.decryptSensitivePayloads(
                any(), eq("key-1"), eq(1_000L), eq("abcdefghijklmnopqrstuv"), eq("MAHAIT_PROFILE")))
                .thenReturn(Map.of(
                        "cinNumber", "l12345mh2020abc123456",
                        "panNumber", "abcde1234f",
                        "gstNumber", "27abcde1234f1z5",
                        "accountNumber", "123456789012",
                        "ifscCode", "test0001234"));
        when(currentActorProvider.getCurrentActorEmail()).thenReturn("admin@example.test");
        when(profileRepository.save(any(MahaItProfile.class))).thenAnswer(invocation -> {
            MahaItProfile entity = invocation.getArgument(0);
            entity.setMahaItProfileId(7L);
            return entity;
        });

        service.create(request);

        ArgumentCaptor<MahaItProfile> entityCaptor = ArgumentCaptor.forClass(MahaItProfile.class);
        verify(profileRepository).save(entityCaptor.capture());
        MahaItProfile saved = entityCaptor.getValue();
        assertThat(saved.getCinNumber()).isEqualTo("L12345MH2020ABC123456");
        assertThat(saved.getPanNumber()).isEqualTo("ABCDE1234F");
        assertThat(saved.getGstNumber()).isEqualTo("27ABCDE1234F1Z5");
        assertThat(saved.getAccountNumber()).isEqualTo("123456789012");
        assertThat(saved.getIfscCode()).isEqualTo("TEST0001234");
        assertThat(request.getCinNumberEncrypted()).isNull();
        assertThat(request.getEncryptionKeyId()).isNull();
        assertThat(request.getCinNumber()).isNull();
        assertThat(request.getAccountNumber()).isNull();

        ArgumentCaptor<String> auditCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).log(eq(7L), eq(MahaItProfileAuditAction.CREATE), auditCaptor.capture());
        assertThat(auditCaptor.getValue())
                .doesNotContain("L12345MH2020ABC123456", "ABCDE1234F", "27ABCDE1234F1Z5", "123456789012");
    }

    @Test
    void createRejectsInjectedPlaintextWhenEncryptedSubmissionIsMissing() {
        MahaItProfileRequest request = validRequest();
        request.clearEncryptedSubmission();
        request.setCinNumber("L12345MH2020ABC123456");
        request.setPanNumber("ABCDE1234F");
        request.setGstNumber("27ABCDE1234F1Z5");
        request.setAccountNumber("123456789012");
        request.setIfscCode("TEST0001234");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to process the submitted MahaIT profile identifiers.");

        verifyNoInteractions(profileRepository, decryptor);
        assertThat(request.getCinNumber()).isNull();
        assertThat(request.getAccountNumber()).isNull();
    }

    private MahaItProfileRequest validRequest() {
        MahaItProfileRequest request = new MahaItProfileRequest();
        request.setCompanyName("MahaIT");
        request.setCompanyAddress("Mumbai");
        request.setBankName("Test Bank");
        request.setBranchName("Main Branch");
        request.setAccountHolderName("MahaIT");
        request.setCinNumberEncrypted("cipher-cin");
        request.setPanNumberEncrypted("cipher-pan");
        request.setGstNumberEncrypted("cipher-gst");
        request.setAccountNumberEncrypted("cipher-account");
        request.setIfscCodeEncrypted("cipher-ifsc");
        request.setEncryptionKeyId("key-1");
        request.setTimestamp(1_000L);
        request.setNonce("abcdefghijklmnopqrstuv");
        return request;
    }
}

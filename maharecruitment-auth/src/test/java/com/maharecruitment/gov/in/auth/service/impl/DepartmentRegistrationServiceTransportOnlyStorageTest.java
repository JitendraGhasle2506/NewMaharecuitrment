package com.maharecruitment.gov.in.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.maharecruitment.gov.in.auth.dto.DepartmentContactRequest;
import com.maharecruitment.gov.in.auth.dto.DepartmentRegistrationRequest;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DepartmentRegistrationServiceTransportOnlyStorageTest {

    @Test
    void storesNormalizedPlaintextWithoutHashOrEncryptedFields() {
        DepartmentRegistrationRepository repository = mock(DepartmentRegistrationRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DepartmentRegistrationServiceImpl service = new DepartmentRegistrationServiceImpl(repository);
        DepartmentRegistrationRequest request = request();

        service.registerDepartment(request);

        ArgumentCaptor<DepartmentRegistrationEntity> captor =
                ArgumentCaptor.forClass(DepartmentRegistrationEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPanNo()).isEqualTo("ABCDE2546F");
        assertThat(captor.getValue().getGstNo()).isEqualTo("27ABCDE1234F1Z5");
        assertThat(DepartmentRegistrationEntity.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("panEncrypted", "gstEncrypted", "panBlindIndex", "gstBlindIndex");
    }

    @Test
    void rejectsInvalidSensitiveIdentityBeforePersistence() {
        DepartmentRegistrationRepository repository = mock(DepartmentRegistrationRepository.class);
        DepartmentRegistrationServiceImpl service = new DepartmentRegistrationServiceImpl(repository);
        DepartmentRegistrationRequest request = request();
        request.setPanNo("INVALID");

        assertThatThrownBy(() -> service.registerDepartment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to process the submitted identity information.");
        verifyNoInteractions(repository);
    }

    private DepartmentRegistrationRequest request() {
        DepartmentRegistrationRequest request = new DepartmentRegistrationRequest();
        request.setDepartmentId(1L);
        request.setSubDeptId(2L);
        request.setDepartmentName("Department");
        request.setAddress("Office address");
        request.setBillDepartmentName("Billing Department");
        request.setPanNo("abcde2546f");
        request.setGstNo("27abcde1234f1z5");
        request.setTanNo("ABCD12345E");
        request.setBillAddress("Billing address");
        request.setTermsConditionAccepted(true);
        request.setPrimaryContact(contact("Primary", "primary@example.test", "9876543210"));
        request.setSecondaryContact(contact("Secondary", "secondary@example.test", "9876543211"));
        return request;
    }

    private DepartmentContactRequest contact(String name, String email, String mobile) {
        DepartmentContactRequest request = new DepartmentContactRequest();
        request.setContactName(name);
        request.setDesignation("Officer");
        request.setEmail(email);
        request.setMobileNo(mobile);
        return request;
    }
}

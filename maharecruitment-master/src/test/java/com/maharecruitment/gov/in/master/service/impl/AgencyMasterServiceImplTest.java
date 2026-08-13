package com.maharecruitment.gov.in.master.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.auth.dto.AgencyUserProvisioningResult;
import com.maharecruitment.gov.in.auth.service.AgencyUserProvisioningService;
import com.maharecruitment.gov.in.master.dto.AgencyEscalationMatrixRequest;
import com.maharecruitment.gov.in.master.dto.AgencyMasterRequest;
import com.maharecruitment.gov.in.master.entity.AgencyBankAccountType;
import com.maharecruitment.gov.in.master.entity.AgencyEntityType;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.mapper.AgencyMasterMapper;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.service.AgencyMasterAuditService;
import com.maharecruitment.gov.in.master.service.AgencyTypeCatalog;
import com.maharecruitment.gov.in.master.service.CurrentActorProvider;

class AgencyMasterServiceImplTest {

    @Test
    void updatePreservesExistingSensitiveIdentityWhenNoReplacementIsSubmitted() {
        AgencyMasterRepository repository = mock(AgencyMasterRepository.class);
        AgencyUserProvisioningService provisioningService = mock(AgencyUserProvisioningService.class);
        AgencyMasterAuditService auditService = mock(AgencyMasterAuditService.class);
        AgencyTypeCatalog agencyTypeCatalog = mock(AgencyTypeCatalog.class);
        CurrentActorProvider actorProvider = mock(CurrentActorProvider.class);
        AgencyMaster existing = existingAgency();

        when(repository.findDetailedByAgencyId(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any(AgencyMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agencyTypeCatalog.isSupported("Recruitment Agency")).thenReturn(true);
        when(agencyTypeCatalog.resolveCanonicalType("Recruitment Agency")).thenReturn("Recruitment Agency");
        when(provisioningService.createOrSyncAgencyUser(any())).thenReturn(AgencyUserProvisioningResult.builder()
                .email("agency@example.test")
                .created(false)
                .build());

        AgencyMasterServiceImpl service = new AgencyMasterServiceImpl(
                repository,
                new AgencyMasterMapper(),
                provisioningService,
                auditService,
                agencyTypeCatalog,
                actorProvider);

        service.update(5L, updateRequestWithoutSensitiveIdentity());

        assertThat(existing.getPanNumber()).isEqualTo("legacyEncryptedPanValue");
        assertThat(existing.getGstNumber()).isEqualTo("legacyEncryptedGstValue");
        assertThat(existing.getBankAccountNumber()).isEqualTo("legacyEncryptedAccountValue");
        verify(repository, never()).existsByPanNumberExcludingId(any(), anyLong());
        verify(repository, never()).existsByGstNumberExcludingId(any(), anyLong());
    }

    private AgencyMaster existingAgency() {
        AgencyMaster agency = new AgencyMaster();
        agency.setAgencyId(5L);
        agency.setOfficialEmail("agency@example.test");
        agency.setPanNumber("legacyEncryptedPanValue");
        agency.setGstNumber("legacyEncryptedGstValue");
        agency.setBankAccountNumber("legacyEncryptedAccountValue");
        agency.setStatus(AgencyStatus.ACTIVE);
        return agency;
    }

    private AgencyMasterRequest updateRequestWithoutSensitiveIdentity() {
        AgencyMasterRequest request = new AgencyMasterRequest();
        request.setAgencyName("Agency Name");
        request.setOfficialEmail("agency@example.test");
        request.setTelephoneNumber("02212345678");
        request.setAgencyType("Recruitment Agency");
        request.setOfficialAddress("Official address");
        request.setPermanentAddress("Permanent address");
        request.setEntityType(AgencyEntityType.PRIVATE_LIMITED);
        request.setPanCopyPath("agency-master/pan/pan.pdf");
        request.setCertificateNumber("CERT-1");
        request.setCertificateDocumentPath("agency-master/certificate/certificate.pdf");
        request.setGstDocumentPath("agency-master/gst/gst.pdf");
        request.setContactPersonName("Contact Person");
        request.setContactPersonMobileNo("9876543210");
        request.setMsmeRegistered(true);
        request.setEscalationMatrixEntries(List.of(escalationEntry()));
        request.setBankName("Example Bank");
        request.setBankBranch("Main Branch");
        request.setBankAccountType(AgencyBankAccountType.CURRENT);
        request.setIfscCode("ABCD0123456");
        request.setCancelledChequePath("agency-master/cancelled-cheque/cheque.pdf");
        return request;
    }

    private AgencyEscalationMatrixRequest escalationEntry() {
        AgencyEscalationMatrixRequest entry = new AgencyEscalationMatrixRequest();
        entry.setContactName("Escalation Contact");
        entry.setMobileNumber("9876543210");
        entry.setLevel("L1");
        entry.setDesignation("Manager");
        entry.setCompanyEmailId("escalation@example.test");
        return entry;
    }
}

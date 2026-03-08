package com.maharecruitment.gov.in.master.mapper;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.AgencyEscalationMatrixResponse;
import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.master.entity.AgencyEscalationMatrix;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;

@Component
public class AgencyMasterMapper {

    public AgencyMasterResponse toResponse(AgencyMaster entity, boolean includeEscalationEntries) {
        AgencyMasterResponse response = new AgencyMasterResponse();
        response.setAgencyId(entity.getAgencyId());
        response.setAgencyName(entity.getAgencyName());
        response.setOfficialEmail(entity.getOfficialEmail());
        response.setTelephoneNumber(entity.getTelephoneNumber());
        response.setAgencyType(entity.getAgencyType());
        response.setOfficialAddress(entity.getOfficialAddress());
        response.setPermanentAddress(entity.getPermanentAddress());
        response.setEntityType(entity.getEntityType());
        response.setPanNumber(entity.getPanNumber());
        response.setPanCopyPath(entity.getPanCopyPath());
        response.setCertificateNumber(entity.getCertificateNumber());
        response.setCertificateDocumentPath(entity.getCertificateDocumentPath());
        response.setGstNumber(entity.getGstNumber());
        response.setGstDocumentPath(entity.getGstDocumentPath());
        response.setContactPersonName(entity.getContactPersonName());
        response.setContactPersonMobileNo(entity.getContactPersonMobileNo());
        response.setMsmeRegistered(entity.getMsmeRegistered());
        response.setBankName(entity.getBankName());
        response.setBankBranch(entity.getBankBranch());
        response.setBankAccountNumber(entity.getBankAccountNumber());
        response.setBankAccountType(entity.getBankAccountType());
        response.setIfscCode(entity.getIfscCode());
        response.setCancelledChequePath(entity.getCancelledChequePath());
        response.setStatus(entity.getStatus());
        response.setCreatedDateTime(entity.getCreatedDateTime());
        response.setUpdatedDateTime(entity.getUpdatedDateTime());
        response.setEscalationMatrixEntries(includeEscalationEntries
                ? toEscalationResponses(entity.getEscalationMatrixEntries())
                : Collections.emptyList());
        return response;
    }

    private List<AgencyEscalationMatrixResponse> toEscalationResponses(List<AgencyEscalationMatrix> entries) {
        return entries.stream().map(this::toEscalationResponse).toList();
    }

    private AgencyEscalationMatrixResponse toEscalationResponse(AgencyEscalationMatrix entity) {
        AgencyEscalationMatrixResponse response = new AgencyEscalationMatrixResponse();
        response.setEscalationMatrixId(entity.getEscalationMatrixId());
        response.setContactName(entity.getContactName());
        response.setMobileNumber(entity.getMobileNumber());
        response.setLevel(entity.getLevel());
        response.setDesignation(entity.getDesignation());
        response.setCompanyEmailId(entity.getCompanyEmailId());
        return response;
    }
}

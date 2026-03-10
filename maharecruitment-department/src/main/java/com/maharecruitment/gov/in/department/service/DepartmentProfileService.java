package com.maharecruitment.gov.in.department.service;

import com.maharecruitment.gov.in.department.dto.DepartmentProfileUpdateForm;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileDocumentType;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileDocumentView;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileView;

public interface DepartmentProfileService {

    DepartmentProfileView getProfile(String actorEmail);

    DepartmentProfileUpdateForm getProfileForEdit(String actorEmail);

    void updateProfile(String actorEmail, DepartmentProfileUpdateForm updateForm);

    DepartmentProfileDocumentView getProfileDocument(String actorEmail, DepartmentProfileDocumentType documentType);
}

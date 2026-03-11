package com.maharecruitment.gov.in.department.service;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationType;

public interface DepartmentRequestIdGenerator {

    String generate(String applicationType);
}

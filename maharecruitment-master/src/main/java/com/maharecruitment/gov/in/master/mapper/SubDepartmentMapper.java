package com.maharecruitment.gov.in.master.mapper;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.SubDepartmentResponse;
import com.maharecruitment.gov.in.master.entity.SubDepartment;

@Component
public class SubDepartmentMapper {

    public SubDepartmentResponse toResponse(SubDepartment entity) {
        return SubDepartmentResponse.builder()
                .subDeptId(entity.getSubDeptId())
                .subDeptName(entity.getSubDeptName())
                .departmentId(entity.getDepartment().getDepartmentId())
                .departmentName(entity.getDepartment().getDepartmentName())
                .createdAt(entity.getCreatedDateTime())
                .updatedAt(entity.getUpdatedDateTime())
                .build();
    }
}

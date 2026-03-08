package com.maharecruitment.gov.in.master.mapper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.maharecruitment.gov.in.master.dto.DepartmentResponse;
import com.maharecruitment.gov.in.master.dto.SubDepartmentSummaryResponse;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(DepartmentMst entity, boolean includeSubDepartments) {
        List<SubDepartmentSummaryResponse> subDepartments = includeSubDepartments
                ? entity.getSubDepartments().stream()
                        .sorted(Comparator.comparing(
                                sub -> sub.getSubDeptName(),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .map(sub -> SubDepartmentSummaryResponse.builder()
                                .subDeptId(sub.getSubDeptId())
                                .subDeptName(sub.getSubDeptName())
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return DepartmentResponse.builder()
                .departmentId(entity.getDepartmentId())
                .departmentName(entity.getDepartmentName())
                .subDepartments(subDepartments)
                .createdAt(entity.getCreatedDateTime())
                .updatedAt(entity.getUpdatedDateTime())
                .build();
    }
}

package com.maharecruitment.gov.in.recruitment.dto.organization;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/** A bounded tree fragment. nextOffset is null when all direct reports are loaded. */
@Getter
@RequiredArgsConstructor
public class EmployeeHierarchyNode {
    private final Long employeeId;
    private final String employeeName;
    private final String employeeCode;
    private final String designation;
    private final String department;
    private final String profilePhoto;
    private final int totalChildren;
    private final boolean filterMatch;
    @Setter
    private Integer nextOffset;
    private final List<EmployeeHierarchyNode> children = new ArrayList<>();

    public boolean isHasChildren() {
        return totalChildren > 0;
    }
}

package com.maharecruitment.gov.in.master.repository;

public interface ProjectCellSummaryProjection {

    String getCellName();

    Long getTotalProjects();

    Long getInternalProjects();

    Long getExternalProjects();
}

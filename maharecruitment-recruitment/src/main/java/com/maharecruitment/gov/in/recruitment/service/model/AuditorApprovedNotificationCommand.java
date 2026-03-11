package com.maharecruitment.gov.in.recruitment.service.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditorApprovedNotificationCommand {

    private String requestId;
    private Long departmentRegistrationId;
    private Long departmentProjectApplicationId;

    @Builder.Default
    private List<DesignationVacancyInput> designationVacancies = new ArrayList<>();
}

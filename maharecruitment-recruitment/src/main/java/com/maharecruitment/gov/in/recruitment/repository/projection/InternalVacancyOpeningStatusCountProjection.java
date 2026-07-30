package com.maharecruitment.gov.in.recruitment.repository.projection;

import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;

public interface InternalVacancyOpeningStatusCountProjection {

    InternalVacancyOpeningStatus getStatus();

    Long getTotalCount();
}

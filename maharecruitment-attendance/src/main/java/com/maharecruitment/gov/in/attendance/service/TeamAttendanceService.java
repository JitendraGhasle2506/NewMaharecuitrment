package com.maharecruitment.gov.in.attendance.service;

import java.time.YearMonth;
import java.util.Optional;

import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceMemberView;
import com.maharecruitment.gov.in.attendance.service.model.TeamAttendanceOverview;

public interface TeamAttendanceService {

    TeamAttendanceOverview getOverview(Long authorityUserId, YearMonth period);

    Optional<TeamAttendanceMemberView> getAuthorizedMember(
            Long authorityUserId,
            Long employeeId,
            YearMonth period);
}

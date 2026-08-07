package com.maharecruitment.gov.in.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.AttendanceRegisterEntity;

@Repository
public interface AttendanceRegisterRepo extends JpaRepository<AttendanceRegisterEntity, Long> {

	Optional<AttendanceRegisterEntity> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);

	@Query(value = """
			select case :day
				when 1 then ad.d1
				when 2 then ad.d2
				when 3 then ad.d3
				when 4 then ad.d4
				when 5 then ad.d5
				when 6 then ad.d6
				when 7 then ad.d7
				when 8 then ad.d8
				when 9 then ad.d9
				when 10 then ad.d10
				when 11 then ad.d11
				when 12 then ad.d12
				when 13 then ad.d13
				when 14 then ad.d14
				when 15 then ad.d15
				when 16 then ad.d16
				when 17 then ad.d17
				when 18 then ad.d18
				when 19 then ad.d19
				when 20 then ad.d20
				when 21 then ad.d21
				when 22 then ad.d22
				when 23 then ad.d23
				when 24 then ad.d24
				when 25 then ad.d25
				when 26 then ad.d26
				when 27 then ad.d27
				when 28 then ad.d28
				when 29 then ad.d29
				when 30 then ad.d30
				when 31 then ad.d31
			end
			from attendance_daily ad
			where ad.user_id = :userId
			  and ad.month = :month
			  and ad.year = :year
			limit 1
			""", nativeQuery = true)
	Optional<String> findDayStatus(
			@Param("userId") Long userId,
			@Param("month") Integer month,
			@Param("year") Integer year,
			@Param("day") Integer day);

	@Query(value = "SELECT " +
			"    u.id AS userId, " +
			"    depProj.request_id AS requestId, " +
			"    u.name AS fullName, " +
			"    depMSt.department_name AS departmentName, " +
			"    subdep.sub_dept_name AS subDepartmentName, " +
			"    depProj.project_name AS applicationName, " +
			"    ad.d1, ad.d2, ad.d3, ad.d4, ad.d5, ad.d6, ad.d7, ad.d8, ad.d9, ad.d10, " +
			"    ad.d11, ad.d12, ad.d13, ad.d14, ad.d15, ad.d16, ad.d17, ad.d18, ad.d19, ad.d20, " +
			"    ad.d21, ad.d22, ad.d23, ad.d24, ad.d25, ad.d26, ad.d27, ad.d28, ad.d29, ad.d30, ad.d31 " +
			"FROM users u " +
			"INNER JOIN department_registration_master deReg " +
			"    ON deReg.department_registration_id = u.department_registration_id " +
			"INNER JOIN department_project_application depProj " +
			"    ON depProj.department_registration_id = u.department_registration_id " +
			"INNER JOIN department_mst depMSt " +
			"    ON depMSt.department_id = deReg.department_id " +
			"INNER JOIN sub_department subdep " +
			"    ON subdep.sub_dept_id = deReg.sub_department_id " +
			"LEFT JOIN attendance_daily ad " +
			"    ON ad.user_id = u.id AND ad.month = :month AND ad.year = :year " +
			"WHERE (:regId IS NULL OR :regId = 0 OR deReg.department_registration_id = :regId) " +
			"AND (:subDeptId IS NULL OR :subDeptId = 0 OR deReg.sub_department_id = :subDeptId) " +
			"AND (:projectId IS NULL OR :projectId = 0 OR depProj.department_project_application_id = :projectId)", nativeQuery = true)
	List<com.maharecruitment.gov.in.attendance.dto.AttendanceReportProjection> getAttendanceReportNative(
			@org.springframework.data.repository.query.Param("regId") Long regId,
			@org.springframework.data.repository.query.Param("subDeptId") Long subDeptId,
			@org.springframework.data.repository.query.Param("projectId") Long projectId,
			@org.springframework.data.repository.query.Param("month") Integer month,
			@org.springframework.data.repository.query.Param("year") Integer year);

	@Query(value = "SELECT " +
			"    em.employee_id AS userId, " +
			"    em.request_id AS requestId, " +
			"    em.full_name AS fullName, " +
			"    depMSt.department_name AS departmentName, " +
			"    agm.agency_name AS subDepartmentName, " +
			"    depProj.project_name AS applicationName, " +
			"    ad.d1, ad.d2, ad.d3, ad.d4, ad.d5, ad.d6, ad.d7, ad.d8, ad.d9, ad.d10, " +
			"    ad.d11, ad.d12, ad.d13, ad.d14, ad.d15, ad.d16, ad.d17, ad.d18, ad.d19, ad.d20, " +
			"    ad.d21, ad.d22, ad.d23, ad.d24, ad.d25, ad.d26, ad.d27, ad.d28, ad.d29, ad.d30, ad.d31, " +
			"    em.level_code AS level, " +
			"    desig.designation_name AS designation " +
			"FROM employee_master em " +
			"INNER JOIN department_registration_master deReg " +
			"    ON deReg.department_registration_id = em.department_registration_id " +
			"INNER JOIN department_mst depMSt " +
			"    ON depMSt.department_id = deReg.department_id " +
			"LEFT JOIN agency_master agm " +
			"    ON agm.agency_id = em.agency_id " +
			"LEFT JOIN department_project_application depProj " +
			"    ON depProj.request_id = em.request_id " +
			"LEFT JOIN manpower_designation_master desig " +
			"    ON desig.designation_id = em.designation_id " +
			"LEFT JOIN attendance_daily ad " +
			"    ON ad.user_id = em.employee_id AND ad.month = :month AND ad.year = :year " +
			"WHERE em.recruitment_type = 'EXTERNAL' " +
			"AND UPPER(TRIM(COALESCE(em.status, ''))) = 'ACTIVE' " +
			"AND (:regId IS NULL OR :regId = 0 OR deReg.department_registration_id = :regId) " +
			"AND (:agencyId IS NULL OR :agencyId = 0 OR agm.agency_id = :agencyId) " +
			"AND (:projectId IS NULL OR :projectId = 0 OR depProj.department_project_application_id = :projectId)", nativeQuery = true)
	List<com.maharecruitment.gov.in.attendance.dto.ExternalAttendanceReportProjection> getExternalAttendanceReportNative(
			@org.springframework.data.repository.query.Param("regId") Long regId,
			@org.springframework.data.repository.query.Param("agencyId") Long agencyId,
			@org.springframework.data.repository.query.Param("projectId") Long projectId,
			@org.springframework.data.repository.query.Param("month") Integer month,
			@org.springframework.data.repository.query.Param("year") Integer year);

	@Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM attendance_daily ad " +
			"INNER JOIN users u ON u.id = ad.user_id " +
			"WHERE u.department_registration_id = :regId AND ad.month = :month AND ad.year = :year", nativeQuery = true)
	boolean existsByDeptRegIdAndMonthAndYear(@Param("regId") Long regId, @Param("month") Integer month, @Param("year") Integer year);

	@Query(value = """
			select count(distinct em.employee_id)
			from attendance_daily ad
			join employee_master em on em.employee_id = ad.user_id
			where upper(trim(coalesce(em.recruitment_type, ''))) = 'EXTERNAL'
			  and upper(trim(coalesce(em.status, ''))) = 'ACTIVE'
			  and trim(coalesce(em.employee_code, '')) <> ''
			  and upper(trim(coalesce(em.employee_code, ''))) <> 'PENDING'
			  and upper(trim(coalesce(em.employee_code, ''))) not like 'TMP-%'
			  and ad.month = :month
			  and ad.year = :year
			  and upper(trim(coalesce(
				case :day
					when 1 then ad.d1
					when 2 then ad.d2
					when 3 then ad.d3
					when 4 then ad.d4
					when 5 then ad.d5
					when 6 then ad.d6
					when 7 then ad.d7
					when 8 then ad.d8
					when 9 then ad.d9
					when 10 then ad.d10
					when 11 then ad.d11
					when 12 then ad.d12
					when 13 then ad.d13
					when 14 then ad.d14
					when 15 then ad.d15
					when 16 then ad.d16
					when 17 then ad.d17
					when 18 then ad.d18
					when 19 then ad.d19
					when 20 then ad.d20
					when 21 then ad.d21
					when 22 then ad.d22
					when 23 then ad.d23
					when 24 then ad.d24
					when 25 then ad.d25
					when 26 then ad.d26
					when 27 then ad.d27
					when 28 then ad.d28
					when 29 then ad.d29
					when 30 then ad.d30
					when 31 then ad.d31
				end, ''))) in ('P', 'PRESENT')
			""", nativeQuery = true)
	long countExternalPresentByMonthYearDay(
			@Param("month") Integer month,
			@Param("year") Integer year,
			@Param("day") Integer day);
}

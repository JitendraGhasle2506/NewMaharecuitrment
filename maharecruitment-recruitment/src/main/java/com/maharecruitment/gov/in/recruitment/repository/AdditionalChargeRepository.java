package com.maharecruitment.gov.in.recruitment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.recruitment.entity.AdditionalCharge;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AdditionalChargeRepository extends JpaRepository<AdditionalCharge, Long> {
    @Query("select r.name from Role r order by lower(r.name), r.id")
    List<String> chargeRoles();

    @Query("select r.name from Role r where lower(r.name) = lower(:name)")
    Optional<String> findChargeRole(@Param("name") String name);

    interface Choice {
        Long getId();
        String getLabel();
    }

    @Query("""
            select e.employeeId as id, concat(e.fullName, ' (', coalesce(e.employeeCode, 'No code'), ')') as label
            from EmployeeEntity e left join e.user u
            where
            """ + EmployeeHierarchyRepository.ACTIVE_EMPLOYEE + " order by lower(e.fullName), e.employeeId")
    List<Choice> employeeChoices();

    @Query("select count(e) > 0 from EmployeeEntity e left join e.user u where e.employeeId = :id and "
            + EmployeeHierarchyRepository.ACTIVE_EMPLOYEE)
    boolean eligibleEmployee(@Param("id") Long id);

    @Query("""
            select u.id as id, concat(u.name, ' (', u.email, ')') as label from User u
            where u.active = true and not exists (select e.employeeId from EmployeeEntity e where e.user = u)
            order by lower(u.name), u.id
            """)
    List<Choice> userChoices();

    @Query("select e.employeeId as id, concat(e.fullName, ' (', coalesce(e.employeeCode, 'No code'), ')') as label from EmployeeEntity e order by lower(e.fullName), e.employeeId")
    List<Choice> replacementEmployees();

    @Query("select u.id as id, concat(u.name, ' (', u.email, ')') as label from User u where not exists (select e.employeeId from EmployeeEntity e where e.user = u) order by lower(u.name), u.id")
    List<Choice> replacementUsers();

    @Query("select u from User u where u.id = :id")
    Optional<User> replacementUser(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CellMaster c join fetch c.wing where c.cellId = :id")
    Optional<CellMaster> lockCell(@Param("id") Long id);

    @Query("""
            select e.employeeId as id, concat(e.fullName, ' (', coalesce(e.employeeCode, 'No code'), ')') as label
            from EmployeeEntity e left join e.user u
            left join EmployeeCellMappingEntity ec on ec.employee = e
            where (:cellId is null or ec.cell.cellId = :cellId) and ( :sourceEmployeeId is null or e.employeeId <> :sourceEmployeeId )
            and
            """ + EmployeeHierarchyRepository.ACTIVE_EMPLOYEE + """
            and (
                exists (select m.mappingId from EmployeeReportingMappingEntity m
                    where m.employeeId = e.employeeId and coalesce(m.reportingType, 'PRIMARY') = 'PRIMARY'
                    and m.mappingId = (select max(latest.mappingId) from EmployeeReportingMappingEntity latest
                        where latest.employeeId = e.employeeId and coalesce(latest.reportingType, 'PRIMARY') = 'PRIMARY')
                    and (m.managerEmployeeId = :sourceEmployeeId
                        or (m.managerEmployeeId is null and m.hodUserId = :sourceUserId)))
                or (not exists (select m.mappingId from EmployeeReportingMappingEntity m
                        where m.employeeId = e.employeeId and coalesce(m.reportingType, 'PRIMARY') = 'PRIMARY')
                    and exists (select cm.mappingId from CellReportingAuthorityMappingEntity cm
                        where cm.cell = ec.cell and cm.authorityLevel = 1 and cm.authorityUserId = :sourceUserId))
            ) order by lower(e.fullName), e.employeeId
            """)
    List<Choice> replacementTeam(@Param("cellId") Long cellId, @Param("sourceEmployeeId") Long employeeId,
                                 @Param("sourceUserId") Long userId);

    @Query("""
            select count(a) > 0 from AdditionalCharge a
            left join a.replacedEmployee e left join a.replacedUser u left join a.cell c
            where a.revokedAt is null and (:cellId is null or c.cellId is null or c.cellId = :cellId)
            and ((:employeeId is not null and e.employeeId = :employeeId)
                 or (:userId is not null and u.id = :userId))
            and a.startDate <= :endDate and (a.endDate is null or a.endDate >= :startDate)
            """)
    boolean replacementOverlaps(@Param("cellId") Long cellId, @Param("employeeId") Long employeeId,
                               @Param("userId") Long userId, @Param("startDate") LocalDate start,
                               @Param("endDate") LocalDate end);

    interface TeamOverride {
        Long getEmployeeId();
        Long getManagerEmployeeId();
    }

    @Query("""
            select member as employeeId, coalesce(e.employeeId, linked.employeeId) as managerEmployeeId
            from AdditionalCharge a join a.teamEmployeeIds member
            left join a.employee e left join e.user eu left join a.user u
            left join EmployeeEntity linked on linked.user = u
            left join a.cell c left join c.wing wing
            where a.revokedAt is null and a.startDate <= :today
            and (a.endDate is null or a.endDate >= :today)
            and (c.cellId is null or (upper(c.activeFlag) = 'Y' and upper(wing.activeFlag) = 'Y'))
            and (eu.id is null or eu.active = true) and (u.id is null or u.active = true)
            and (c.cellId is null or exists (select ec.employeeCellMappingId from EmployeeCellMappingEntity ec
                where ec.employee.employeeId = member and ec.cell = c))
            order by a.id desc
            """)
    List<TeamOverride> activeTeamOverrides(@Param("today") LocalDate today);

    interface ChargeLabel {
        Long getEmployeeId();
        String getPosition();
        String getReplacedName();
        String getCellName();
    }

    @Query("""
            select coalesce(e.employeeId, linked.employeeId) as employeeId, a.position as position,
                   coalesce(source.fullName, sourceUser.name) as replacedName,
                   coalesce(c.cellName, d.departmentName, 'All cells') as cellName
            from AdditionalCharge a left join a.employee e left join a.user u
            left join EmployeeEntity linked on linked.user = u
            left join a.replacedEmployee source left join a.replacedUser sourceUser
            left join a.cell c left join c.wing wing left join a.department d
            where a.revokedAt is null and a.startDate <= :today and (a.endDate is null or a.endDate >= :today)
              and (c.cellId is null or (upper(c.activeFlag) = 'Y' and upper(wing.activeFlag) = 'Y'))
            order by a.id desc
            """)
    List<ChargeLabel> activeChargeLabels(@Param("today") LocalDate today);

    interface PersonalCharge {
        String getPosition();
        String getReplacedName();
        String getScope();
        LocalDate getStartDate();
        LocalDate getEndDate();
        String getResponsibilities();
    }

    @Query("""
            select a.position as position, coalesce(source.fullName, sourceUser.name) as replacedName,
                   coalesce(c.cellName, d.departmentName, 'All cells') as scope,
                   a.startDate as startDate, a.endDate as endDate, a.responsibilities as responsibilities
            from AdditionalCharge a left join a.employee e left join e.user eu left join a.user u
            left join a.replacedEmployee source left join a.replacedUser sourceUser
            left join a.cell c left join c.wing wing left join a.department d
            where (lower(eu.email) = lower(:email) or lower(u.email) = lower(:email))
              and a.revokedAt is null and a.startDate <= :today and (a.endDate is null or a.endDate >= :today)
              and (c.cellId is null or (upper(c.activeFlag) = 'Y' and upper(wing.activeFlag) = 'Y'))
            order by a.id desc
            """)
    List<PersonalCharge> personalActiveCharges(@Param("email") String email, @Param("today") LocalDate today);

    interface ApprovalCharge {
        Long getChargeId();
        Long getEmployeeId();
        Long getHolderEmployeeId();
        Long getHolderUserId();
        Long getSourceUserId();
        Long getCellId();
    }

    @Query("""
            select a.id as chargeId, member as employeeId,
                   coalesce(e.employeeId, linked.employeeId) as holderEmployeeId,
                   coalesce(eu.id, u.id) as holderUserId,
                   coalesce(sourceAccount.id, sourceUser.id) as sourceUserId, c.cellId as cellId
            from AdditionalCharge a join a.teamEmployeeIds member
            left join a.employee e left join e.user eu left join a.user u
            left join EmployeeEntity linked on linked.user = u
            left join a.replacedEmployee source left join source.user sourceAccount left join a.replacedUser sourceUser
            left join a.cell c left join c.wing wing
            where a.revokedAt is null and a.startDate <= :today and (a.endDate is null or a.endDate >= :today)
            and (c.cellId is null or (upper(c.activeFlag) = 'Y' and upper(wing.activeFlag) = 'Y'))
            and coalesce(eu.id, u.id) is not null and coalesce(sourceAccount.id, sourceUser.id) is not null
            and (eu.id is null or eu.active = true) and (u.id is null or u.active = true)
            and upper(coalesce(e.status, linked.status)) = 'ACTIVE'
            and not exists (select r.relievingId from EmployeeRelievingEntity r
                where (r.employee = e or r.employee = linked)
                and upper(coalesce(r.status, 'INITIATED')) not in ('CANCELLED', 'REJECTED')
                and (upper(r.status) in ('COMPLETED', 'RELIEVED') or r.exitDate <= :today))
            order by a.id desc
            """)
    List<ApprovalCharge> activeApprovalCharges(@Param("today") LocalDate today);

    interface CellMember {
        Long getEmployeeId();
        Long getCellId();
    }

    @Query("select ec.employee.employeeId as employeeId, ec.cell.cellId as cellId from EmployeeCellMappingEntity ec where ec.cell.cellId in :cellIds")
    List<CellMember> approvalCellMembers(@Param("cellIds") java.util.Collection<Long> cellIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmployeeEntity e where e.employeeId = :id")
    Optional<EmployeeEntity> lockEmployee(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockUser(@Param("id") Long id);

    @EntityGraph(attributePaths = {"employee", "user", "department", "cell", "replacedEmployee", "replacedUser"})
    Page<AdditionalCharge> findAllByOrderByIdDesc(Pageable pageable);

    @Query("""
            select count(a) > 0 from AdditionalCharge a
            left join a.employee e left join a.user u left join a.department d left join a.cell c
            where a.revokedAt is null
            and ((:employeeId is not null and e.employeeId = :employeeId)
                 or (:userId is not null and u.id = :userId))
            and lower(a.position) = lower(:position)
            and ((:departmentId is not null and d.departmentId = :departmentId)
                 or (:departmentId is null and d.departmentId is null
                     and (:cellId is null or c.cellId is null or c.cellId = :cellId)))
            and a.startDate <= :endDate and (a.endDate is null or a.endDate >= :startDate)
            """)
    boolean overlaps(@Param("employeeId") Long employeeId, @Param("userId") Long userId,
                     @Param("position") String position, @Param("departmentId") Long departmentId,
                     @Param("cellId") Long cellId, @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);
}

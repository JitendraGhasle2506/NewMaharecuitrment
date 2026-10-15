package com.maharecruitment.gov.in.recruitment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.AdditionalCharge;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.AdditionalChargeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class AdditionalChargeServiceTest {
    final AdditionalChargeRepository repository = mock(AdditionalChargeRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final CellMasterRepository cells = mock(CellMasterRepository.class);
    final AdditionalChargeService service = new AdditionalChargeService(repository, employees, cells);
    final LocalDate today = LocalDate.now();

    @BeforeEach void setupRole() {
        when(repository.findChargeRole("ROLE_HOD")).thenReturn(Optional.of("ROLE_HOD"));
        var cell = new CellMaster();
        cell.setCellId(2L);
        cell.setActiveFlag("Y");
        var wing = new WingMaster();
        wing.setActiveFlag("Y");
        cell.setWing(wing);
        when(repository.lockCell(2L)).thenReturn(Optional.of(cell));
        when(cells.findByCellId(2L)).thenReturn(Optional.of(cell));
        when(repository.lockEmployee(3L)).thenAnswer(unused -> employees.findById(3L));
    }

    AdditionalChargeService.Request request(String assignee, LocalDate end) {
        return new AdditionalChargeService.Request(assignee, " ROLE_HOD ", "C:2", today, end, "Order 12", "Finance duties");
    }

    EmployeeEntity employee() {
        var employee = new EmployeeEntity();
        employee.setEmployeeId(1L);
        employee.setFullName("Employee One");
        employee.setStatus("ACTIVE");
        return employee;
    }

    void setupEmployee() {
        when(repository.lockEmployee(1L)).thenReturn(Optional.of(employee()));
        when(repository.eligibleEmployee(1L)).thenReturn(true);
    }

    @Test
    void assigningLinkedUserUsesEmployeeIdentityAndPreservesPrimaryAssignment() {
        setupEmployee();
        when(employees.findByUser_Id(7L)).thenReturn(Optional.of(employee()));
        service.assign(request("U:7", null), "hr@example.test");
        var captor = ArgumentCaptor.forClass(AdditionalCharge.class);
        verify(repository).save(captor.capture());
        var charge = captor.getValue();
        assertThat(charge.getEmployee().getEmployeeId()).isEqualTo(1L);
        assertThat(charge.getUser()).isNull();
        assertThat(charge.getPosition()).isEqualTo("ROLE_HOD");
        assertThat(charge.getAssignedBy()).isEqualTo("hr@example.test");
        verify(employees, never()).save(any());
    }

    @Test
    void activeUserWithoutEmployeeCanReceiveCharge() {
        var user = new User();
        user.setId(7L);
        user.setActive(true);
        when(employees.findByUser_Id(7L)).thenReturn(Optional.empty());
        when(repository.lockUser(7L)).thenReturn(Optional.of(user));
        service.assign(request("U:7", today.plusDays(10)), "hr");
        var captor = ArgumentCaptor.forClass(AdditionalCharge.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getEmployee()).isNull();
    }

    @Test
    void reversedDatesAndInvalidIdsAreRejectedWithoutSaving() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(request("E:1", today.minusDays(1)), "hr"));
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(request("E:9999999999999999999999", null), "hr"));
        verify(repository, never()).save(any());
    }

    @Test
    void relievedEmployeeAndInactiveUserCannotReceiveCharge() {
        when(repository.lockEmployee(1L)).thenReturn(Optional.of(employee()));
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(request("E:1", null), "hr"));
        var user = new User();
        user.setActive(false);
        when(repository.lockUser(7L)).thenReturn(Optional.of(user));
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(request("U:7", null), "hr"));
        verify(repository, never()).save(any());
    }

    @Test
    void overlappingChargeIsRejected() {
        setupEmployee();
        when(repository.overlaps(1L, null, "ROLE_HOD", null, 2L, today, LocalDate.of(9999, 12, 31))).thenReturn(true);
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(request("E:1", null), "hr"))
                .withMessageContaining("overlapping dates");
        verify(repository, never()).save(any());
    }

    @Test
    void replacementCapturesCellTeamAndExcludesTheChargeHolder() {
        setupEmployee();
        var target = employee();
        var targetAccount = new User();
        targetAccount.setId(11L);
        targetAccount.setActive(true);
        target.setUser(targetAccount);
        when(repository.lockEmployee(1L)).thenReturn(Optional.of(target));
        var source = employee();
        source.setEmployeeId(3L);
        var sourceUser = new User();
        sourceUser.setId(13L);
        source.setUser(sourceUser);
        when(employees.findById(3L)).thenReturn(Optional.of(source));
        var holder = mock(AdditionalChargeRepository.Choice.class);
        when(holder.getId()).thenReturn(1L);
        var member = mock(AdditionalChargeRepository.Choice.class);
        when(member.getId()).thenReturn(5L);
        when(repository.replacementTeam(2L, 3L, 13L)).thenReturn(List.of(holder, member));
        service.assign(new AdditionalChargeService.Request("E:1", "ROLE_HOD", "C:2", today, null, "", "", "E:3"), "hr");
        var captor = ArgumentCaptor.forClass(AdditionalCharge.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReplacedEmployee()).isSameAs(source);
        assertThat(captor.getValue().getTeamEmployeeIds()).containsExactly(5L);
        verify(employees, never()).save(any());
    }

    @Test
    void replacementCannotCoverTheSamePersonOrDuplicateCoverage() {
        setupEmployee();
        when(employees.findById(1L)).thenReturn(Optional.of(employee()));
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(
                new AdditionalChargeService.Request("E:1", "ROLE_HOD", "C:2", today, null, "", "", "E:1"), "hr"))
                .withMessageContaining("different people");
        var source = employee();
        source.setEmployeeId(3L);
        when(employees.findById(3L)).thenReturn(Optional.of(source));
        when(repository.replacementOverlaps(2L, 3L, null, today, LocalDate.of(9999, 12, 31))).thenReturn(true);
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(
                new AdditionalChargeService.Request("E:1", "ROLE_HOD", "C:2", today, null, "", "", "E:3"), "hr"))
                .withMessageContaining("replacement coverage");
        verify(repository, never()).save(any());
    }

    @Test
    void departmentScopeIsRejectedEvenForForgedRequests() {
        setupEmployee();
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(
                new AdditionalChargeService.Request("E:1", "ROLE_HOD", "D:2", today, null, "", ""), "hr"))
                .withMessageContaining("Select a valid cell");
        verify(repository, never()).save(any());
    }

    @Test
    void userWithoutEmployeeRecordMustBeLinkedBeforeInheritingATeam() {
        var target = new User();
        target.setId(7L);
        target.setActive(true);
        when(repository.lockUser(7L)).thenReturn(Optional.of(target));
        var source = employee();
        source.setEmployeeId(3L);
        when(employees.findById(3L)).thenReturn(Optional.of(source));
        var member = mock(AdditionalChargeRepository.Choice.class);
        when(member.getId()).thenReturn(5L);
        when(repository.replacementTeam(2L, 3L, null)).thenReturn(List.of(member));
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(
                new AdditionalChargeService.Request("U:7", "ROLE_HOD", "C:2", today, null, "", "", "E:3"), "hr"))
                .withMessageContaining("Link the selected user");
        verify(repository, never()).save(any());
    }

    @Test
    void replacementPreviewResolvesALinkedUserToTheEmployeeIdentity() {
        var source = employee();
        source.setEmployeeId(3L);
        var user = new User();
        user.setId(13L);
        source.setUser(user);
        when(employees.findByUser_Id(13L)).thenReturn(Optional.of(source));
        service.previewTeam("C:2", "U:13");
        verify(repository).replacementTeam(2L, 3L, 13L);
    }

    @Test
    void optionalCellCapturesTheReplacedPersonsTeamAcrossAllCells() {
        setupEmployee();
        var target = employee();
        var account = new User();
        account.setId(11L);
        account.setActive(true);
        target.setUser(account);
        when(repository.lockEmployee(1L)).thenReturn(Optional.of(target));
        var source = employee();
        source.setEmployeeId(3L);
        when(employees.findById(3L)).thenReturn(Optional.of(source));
        var member = mock(AdditionalChargeRepository.Choice.class);
        when(member.getId()).thenReturn(5L);
        when(repository.replacementTeam(null, 3L, null)).thenReturn(List.of(member));
        service.assign(new AdditionalChargeService.Request("E:1", "ROLE_HOD", "", today, null, "", "", "E:3"), "hr");
        var captor = ArgumentCaptor.forClass(AdditionalCharge.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCell()).isNull();
        assertThat(captor.getValue().getTeamEmployeeIds()).containsExactly(5L);
        verify(repository, never()).lockCell(any());
        verify(repository).replacementOverlaps(null, 3L, null, today, LocalDate.of(9999, 12, 31));
        assertThat(service.previewTeam("", "E:3")).containsExactly(member);
    }

    @Test
    void globalChargeWithoutReplacementIsSavedAndListedAsAllCells() {
        setupEmployee();
        service.assign(new AdditionalChargeService.Request("E:1", "ROLE_HOD", null, today, null, "", ""), "hr");
        var captor = ArgumentCaptor.forClass(AdditionalCharge.class);
        verify(repository).save(captor.capture());
        var charge = captor.getValue();
        when(repository.findAllByOrderByIdDesc(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(charge)));
        assertThat(service.list(0).getContent().getFirst().scope()).isEqualTo("All cells");
        assertThat(charge.getTeamEmployeeIds()).isEmpty();
    }

    @Test
    void replacementIdentityLocksFollowAConsistentOrderAcrossGlobalAndCellScopes() {
        var source = employee();
        when(employees.findById(1L)).thenReturn(Optional.of(source));
        when(repository.lockEmployee(1L)).thenReturn(Optional.of(source));
        var holder = employee();
        holder.setEmployeeId(3L);
        when(repository.lockEmployee(3L)).thenReturn(Optional.of(holder));
        when(repository.eligibleEmployee(3L)).thenReturn(true);
        service.assign(new AdditionalChargeService.Request("E:3", "ROLE_HOD", "", today, null, "", "", "E:1"), "hr");
        var order = inOrder(repository);
        order.verify(repository).lockEmployee(1L);
        order.verify(repository).lockEmployee(3L);
        verify(repository).save(any());
    }

    @Test
    void unavailableRoleCannotBeAssigned() {
        setupEmployee();
        when(repository.findChargeRole("ROLE_HOD")).thenReturn(Optional.empty());
        assertThatIllegalArgumentException().isThrownBy(() -> service.assign(request("E:1", null), "hr"))
                .withMessageContaining("Selected role is no longer available");
        verify(repository, never()).save(any());
    }

    @Test
    void revokeKeepsHistoryAndRecordsActor() {
        var charge = new AdditionalCharge();
        when(repository.findById(9L)).thenReturn(Optional.of(charge));
        service.revoke(9L, "hr");
        assertThat(charge.getRevokedAt()).isNotNull();
        assertThat(charge.getRevokedBy()).isEqualTo("hr");
        verify(repository).save(charge);
        assertThatIllegalArgumentException().isThrownBy(() -> service.revoke(9L, "hr"));
        verify(repository, never()).delete(any());
    }

    @Test
    void registerComputesEffectiveStatusesIncludingInclusiveEndDate() {
        var records = java.util.stream.IntStream.range(0, 4).mapToObj(i -> {
            var charge = new AdditionalCharge();
            charge.setEmployee(employee());
            charge.setDepartment(new DepartmentMst());
            charge.setStartDate(i == 1 ? today.plusDays(1) : today.minusDays(2));
            charge.setEndDate(i == 2 ? today.minusDays(1) : today);
            if (i == 3) charge.setRevokedAt(LocalDateTime.now());
            return charge;
        }).toList();
        when(repository.findAllByOrderByIdDesc(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(records));
        assertThat(service.list(0)).extracting(AdditionalChargeService.Entry::status)
                .containsExactly("Active", "Scheduled", "Expired", "Revoked");
    }
}

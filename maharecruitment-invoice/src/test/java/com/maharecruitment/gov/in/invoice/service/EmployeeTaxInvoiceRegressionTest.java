package com.maharecruitment.gov.in.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfile;
import com.maharecruitment.gov.in.common.mahaitprofile.repository.MahaItProfileRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentTaxRateMasterEntity;
import com.maharecruitment.gov.in.department.repository.*;
import com.maharecruitment.gov.in.invoice.dto.*;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.repository.DepartmentTaxInvoiceRepository;
import com.maharecruitment.gov.in.invoice.repository.EmployeeTaxInvoiceRepository.InvoicedEmployee;
import com.maharecruitment.gov.in.invoice.service.impl.DepartmentTaxInvoiceGenerationServiceImpl;
import com.maharecruitment.gov.in.master.entity.*;
import com.maharecruitment.gov.in.master.repository.*;
import com.maharecruitment.gov.in.recruitment.entity.*;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeProjectMappingRepository;

class EmployeeTaxInvoiceRegressionTest {
    private final DepartmentRegistrationRepository registrations = mock(DepartmentRegistrationRepository.class);
    private final DepartmentProjectApplicationRepository applications = mock(DepartmentProjectApplicationRepository.class);
    private final DepartmentTaxRateMasterRepository taxes = mock(DepartmentTaxRateMasterRepository.class);
    private final MahaItProfileRepository profiles = mock(MahaItProfileRepository.class);
    private final ManpowerDesignationRateRepository rates = mock(ManpowerDesignationRateRepository.class);
    private final RateMasterRepository commissions = mock(RateMasterRepository.class);
    private final DepartmentMstRepository departments = mock(DepartmentMstRepository.class);
    private final ProjectMstRepository projects = mock(ProjectMstRepository.class);
    private final EmployeeProjectMappingRepository mappings = mock(EmployeeProjectMappingRepository.class);
    private final EmployeeTaxInvoiceService invoicedLookup = mock(EmployeeTaxInvoiceService.class);
    private EmployeeTaxInvoiceBuilder builder;
    private DepartmentTaxInvoiceGenerationServiceImpl service;
    private ProjectMst project;
    private DepartmentRegistrationEntity registration;
    private final LocalDate start = LocalDate.of(2026, 9, 1);
    private final LocalDate end = LocalDate.of(2026, 9, 30);

    @BeforeEach
    void setup() {
        TaxInvoiceDisplayFormatter formatter = new TaxInvoiceDisplayFormatter();
        IndianCurrencyToWordsConverter words = new IndianCurrencyToWordsConverter();
        builder = new EmployeeTaxInvoiceBuilder(registrations, applications, taxes, profiles, rates,
                commissions, new TaxInvoiceAmountCalculator(), words, formatter,
                new TaxInvoiceViewMapper(words, formatter, new TaxInvoiceQrCodeGenerator()));
        service = new DepartmentTaxInvoiceGenerationServiceImpl(departments, mock(SubDepartmentRepository.class),
                projects, mappings, applications, mock(DepartmentProjectApplicationActivityRepository.class),
                mock(DepartmentTaxInvoiceRepository.class), mock(DepartmentTaxInvoiceService.class), builder,
                invoicedLookup);
        project = new ProjectMst();
        project.setProjectId(2L);
        project.setProjectName("Project");
        project.setDepartmentId(1L);
        registration = new DepartmentRegistrationEntity();
        registration.setDepartmentRegistrationId(3L);
        registration.setDepartmentName("Registered department");
        registration.setBillDepartmentName("Billing department");
        registration.setBillAddress("Billing address");
        registration.setGstNo("27AAKCM6988L1ZG");
        MahaItProfile profile = mock(MahaItProfile.class, invocation ->
                invocation.getMethod().getReturnType() == String.class ? "Configured" : RETURNS_DEFAULTS.answer(invocation));
        when(profile.getPanNumber()).thenReturn("ABCDE1545T");
        when(profile.getGstNumber()).thenReturn("27ABCDE1545TK1Z7");
        when(profiles.findFirstByActiveTrueOrderByUpdatedDateDesc()).thenReturn(Optional.of(profile));
        when(rates.findActiveRatesForPeriod(anyLong(), anyString(), any(), any())).thenReturn(List.of(
                ManpowerDesignationRate.builder().effectiveFrom(start.minusYears(1)).grossMonthlyCtc(new BigDecimal("30000")).build()));
        DepartmentTaxRateMasterEntity cgst = new DepartmentTaxRateMasterEntity();
        cgst.setTaxCode("CGST"); cgst.setRatePercentage(new BigDecimal("9"));
        DepartmentTaxRateMasterEntity sgst = new DepartmentTaxRateMasterEntity();
        sgst.setTaxCode("SGST"); sgst.setRatePercentage(new BigDecimal("9"));
        when(taxes.findApplicableTaxRates(any())).thenReturn(List.of(cgst, sgst));
        when(departments.existsById(1L)).thenReturn(true);
        when(projects.existsById(2L)).thenReturn(true);
        when(projects.findById(2L)).thenReturn(Optional.of(project));
        when(projects.findProjectsByDepartmentIdNative(1L)).thenReturn(List.of(project));
    }

    private EmployeeProjectMappingEntity mapping(long employeeId, LocalDate onboard, LocalDate resign) {
        ManpowerDesignationMaster designation = new ManpowerDesignationMaster();
        designation.setDesignationId(4L); designation.setDesignationName("Developer");
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId); employee.setFullName("Same Name");
        employee.setDesignation(designation); employee.setLevelCode("L1");
        employee.setDepartmentRegistration(registration);
        employee.setOnboardingDate(onboard); employee.setResignationDate(resign);
        EmployeeProjectMappingEntity mapping = new EmployeeProjectMappingEntity();
        mapping.setEmployee(employee); mapping.setProject(project);
        return mapping;
    }

    private TaxInvoiceGenerationFilter filter() {
        TaxInvoiceGenerationFilter filter = new TaxInvoiceGenerationFilter();
        filter.setDepartmentId(1L); filter.setProjectId(2L); filter.setStartDate(start); filter.setEndDate(end);
        return filter;
    }

    @Test
    void repeatedIdsAreBilledOnceWhileSameNamesRemainSeparateInBothPaths() {
        List<EmployeeProjectMappingEntity> duplicated = List.of(mapping(10, null, null),
                mapping(10, null, null), mapping(11, null, null));
        when(mappings.findCurrentProjectEmployeesForTaxInvoice(1L, null, 2L)).thenReturn(duplicated);
        assertThat(service.loadProjectEmployees(filter())).extracting(TaxInvoiceEmployeePreviewView::getEmployeeId)
                .containsExactly(10L, 11L);
        TaxInvoiceView invoice = service.buildEmployeeInvoice(filter());
        assertThat(invoice.getLineItems()).hasSize(2).allSatisfy(line -> {
            assertThat(line.getDescription()).isEqualTo("Same Name - Developer");
            assertThat(line.getQuantity()).isEqualTo(1);
            assertThat(line.getTotalAmount()).isEqualByComparingTo("36300");
        });
        assertThat(invoice.getBaseAmount()).isEqualByComparingTo("72600");
        assertThat(invoice.getCgstAmount()).isEqualByComparingTo("6534");
        assertThat(invoice.getSgstAmount()).isEqualByComparingTo("6534");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("85668");
        assertThat(invoice.getClientGstNumber()).isEqualTo("27AAKCM6988L1ZG");
        assertThat(invoice.getPanNumber()).isEqualTo("ABCDE1545T");
        assertThat(invoice.getGstNumber()).isEqualTo("27ABCDE1545TK1Z7");
        assertThat(invoice.getDocumentTitle()).isEqualTo("TAX INVOICE");
        assertThat(invoice.getBilledTo()).isEqualTo("Billing department");
        assertThat(invoice.getBillingAddress()).isEqualTo("Billing address");
    }

    @Test
    void duplicateMappingsDoNotChangeProrationCommissionsTaxesOrTotals() {
        EmployeeProjectMappingEntity employee = mapping(10, start.plusDays(10), end.minusDays(5));
        TaxInvoiceView single = builder.build(project, 1L, null, List.of(employee), start, end);
        TaxInvoiceView duplicate = builder.build(project, 1L, null, List.of(employee, employee), start, end);
        assertThat(duplicate.getLineItems()).hasSize(1);
        assertThat(single.getBaseAmount()).isEqualByComparingTo("18150");
        assertThat(duplicate.getBaseAmount()).isEqualTo(single.getBaseAmount());
        assertThat(duplicate.getAgencyCommissionAmount()).isEqualByComparingTo("1500");
        assertThat(duplicate.getMahaItCommissionAmount()).isEqualByComparingTo("1650");
        assertThat(duplicate.getTotalAmount()).isEqualByComparingTo("21417");
        assertThat(duplicate.getLineItems().getFirst().getRequiredPeriodDisplay()).isEqualTo("11-09-2026 to 25-09-2026");
    }

    @Test
    void missingBillingAddressIsLeftForUserEntryAndNoGstRemainsOptional() {
        registration.setBillAddress(null); registration.setAddress(null); registration.setGstNo(null);
        TaxInvoiceView invoice = builder.build(project, 1L, null, List.of(mapping(10, null, null)), start, end);
        assertThat(invoice.getBillingAddress()).isEmpty();
        assertThat(invoice.getClientGstNumber()).isNull();
        assertThat(invoice.isClientGstinAvailable()).isFalse();
    }
    @Test
    void unregisteredDepartmentCanPreviewWithoutInventedBillingDetails() {
        EmployeeProjectMappingEntity employee = mapping(10, null, null);
        employee.getEmployee().setDepartmentRegistration(null);
        TaxInvoiceView invoice = builder.build(project, 1L, null, List.of(employee), start, end);
        assertThat(invoice.getDepartmentRegistrationId()).isNull();
        assertThat(invoice.getBilledTo()).isEmpty();
        assertThat(invoice.getBillingAddress()).isEmpty();
        assertThat(invoice.getClientGstNumber()).isNull();
        assertThat(invoice.isClientGstinAvailable()).isFalse();
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("42834");
    }

    @Test
    void unregisteredDepartmentPrefillsKnownDepartmentName() {
        DepartmentMst department = new DepartmentMst();
        department.setDepartmentName("Known department");
        project.setDepartment(department);
        EmployeeProjectMappingEntity employee = mapping(10, null, null);
        employee.getEmployee().setDepartmentRegistration(null);
        TaxInvoiceView invoice = builder.build(project, 1L, null, List.of(employee), start, end);
        assertThat(invoice.getBilledTo()).isEqualTo("Known department");
        assertThat(invoice.getBillingAddress()).isEmpty();
    }

    @Test
    void employeesAlreadyInvoicedForThePeriodAreMarkedAndLeftOffTheInvoice() {
        when(mappings.findCurrentProjectEmployeesForTaxInvoice(1L, null, 2L))
                .thenReturn(List.of(mapping(10, null, null), mapping(11, null, null)));
        when(invoicedLookup.findInvoicedEmployees(List.of(10L, 11L), start, end)).thenReturn(Map.of(10L,
                new InvoicedEmployee(10L, "TI-2026-27-00001", start, start.plusDays(14))));
        List<TaxInvoiceEmployeePreviewView> loaded = service.loadProjectEmployees(filter());
        assertThat(loaded.get(0).getAlreadyInvoicedIn()).isEqualTo("TI-2026-27-00001, 01-09-2026 to 15-09-2026");
        assertThat(loaded.get(0).isBillable()).isFalse();
        assertThat(loaded.get(1).isBillable()).isTrue();

        TaxInvoiceView invoice = service.buildEmployeeInvoice(filter());
        assertThat(invoice.getLineItems()).singleElement().satisfies(line -> {
            assertThat(line.getEmployeeId()).isEqualTo(11L);
            assertThat(line.getEmployeeName()).isEqualTo("Same Name");
            assertThat(line.getDesignationName()).isEqualTo("Developer");
            assertThat(line.getLevelCode()).isEqualTo("L1");
            assertThat(line.getBilledFrom()).isEqualTo(start);
            assertThat(line.getBilledTo()).isEqualTo(end);
        });
    }

    @Test
    void previewFailsWhenEveryEmployeeIsAlreadyInvoiced() {
        when(mappings.findCurrentProjectEmployeesForTaxInvoice(1L, null, 2L)).thenReturn(List.of(mapping(10, null, null)));
        when(invoicedLookup.findInvoicedEmployees(List.of(10L), start, end)).thenReturn(Map.of(10L,
                new InvoicedEmployee(10L, "TI-2026-27-00001", start, end)));
        assertThatThrownBy(() -> service.buildEmployeeInvoice(filter())).isInstanceOf(TaxInvoiceException.class)
                .hasMessage("All employees of the selected project are already invoiced for 01-09-2026 to 30-09-2026.");
    }
}

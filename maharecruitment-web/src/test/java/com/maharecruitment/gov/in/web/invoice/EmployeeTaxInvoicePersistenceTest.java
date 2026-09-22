package com.maharecruitment.gov.in.web.invoice;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.maharecruitment.gov.in.invoice.dto.*;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceNotFoundException;
import com.maharecruitment.gov.in.invoice.repository.EmployeeTaxInvoiceRepository;
import com.maharecruitment.gov.in.invoice.service.EmployeeTaxInvoiceService;
import com.maharecruitment.gov.in.invoice.service.TaxInvoiceNumberGenerator;
import db.postmigration.V128__employee_tax_invoice_storage;

/** Run against an isolated PostgreSQL instance with -Dinvoice.test.databaseUrl=jdbc:postgresql://... . */
@EnabledIfSystemProperty(named = "invoice.test.databaseUrl", matches = ".+")
class EmployeeTaxInvoicePersistenceTest {
    private JdbcTemplate admin;
    private JdbcTemplate jdbc;
    private String schema;
    private EmployeeTaxInvoiceService service;
    private TaxInvoiceNumberGenerator numbers;
    private TaxInvoiceView preview;
    private TaxInvoiceBillingDetails details;
    private TaxInvoiceGenerationFilter filter;
    private final AtomicInteger sequence = new AtomicInteger();

    @BeforeEach
    void setup() throws Exception {
        String url = System.getProperty("invoice.test.databaseUrl");
        String user = System.getProperty("invoice.test.databaseUser", "invoice_test");
        String password = System.getProperty("invoice.test.databasePassword", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, user, password);
        admin = new JdbcTemplate(new DriverManagerDataSource(url, user, password));
        schema = "invoice_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("create schema " + schema);
        Properties properties = new Properties();
        properties.setProperty("currentSchema", schema);
        dataSource.setConnectionProperties(properties);
        jdbc = new JdbcTemplate(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);
            V128__employee_tax_invoice_storage migration = new V128__employee_tax_invoice_storage();
            migration.migrate(context);
            migration.migrate(context);
        }
        numbers = mock(TaxInvoiceNumberGenerator.class);
        when(numbers.generate(any())).thenAnswer(call -> "TI-2026-27-" + sequence.incrementAndGet());
        EmployeeTaxInvoiceService target = new EmployeeTaxInvoiceService(
                new EmployeeTaxInvoiceRepository(new NamedParameterJdbcTemplate(dataSource)), numbers,
                JsonMapper.builder().findAndAddModules().build());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        service = (EmployeeTaxInvoiceService) proxy.getProxy();
        preview = TaxInvoiceView.builder().tiNumber("DRAFT").projectName("Project 100%_done")
                .tiDate(LocalDate.now()).deptRefDate(LocalDate.of(2026, 9, 1)).billedTo("Department")
                .billingAddress("Billing address").requestId("REQ-1").placeOfSupply("Maharashtra")
                .totalAmount(new BigDecimal("1180.00")).totalAmountDisplay("1,180.00")
                .lineItems(List.of(TaxInvoiceLineItemView.builder().lineNumber(1)
                        .description("Employee One - Developer").totalAmount(new BigDecimal("1000.00")).build()))
                .build();
        details = TaxInvoiceBillingDetails.from(preview);
        filter = new TaxInvoiceGenerationFilter();
        filter.setDepartmentId(1L);
        filter.setProjectId(2L);
        filter.setStartDate(LocalDate.of(2026, 9, 1));
        filter.setEndDate(LocalDate.of(2026, 9, 30));
    }

    @AfterEach
    void cleanup() {
        if (admin != null && schema != null) {
            admin.execute("drop schema " + schema + " cascade");
        }
    }

    private long generate(String token) {
        return service.generate(token, filter, preview, details, "hr@example.com");
    }

    @Test
    void savesSnapshotListsItAndReopensWithoutOriginalPreview() {
        details.setBillingAddress("Line one\nLine two");
        long id = generate(UUID.randomUUID().toString());
        preview.getLineItems().getFirst().setDescription("Changed after generation");
        preview.setTotalAmount(BigDecimal.ONE);
        TaxInvoiceView saved = service.getInvoice(id);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("1180.00");
        assertThat(saved.getBillingAddress()).isEqualTo("Line one\nLine two");
        assertThat(saved.getLineItems().getFirst().getDescription()).isEqualTo("Employee One - Developer");
        assertThat(saved.getGeneratedByLoginId()).isEqualTo("hr@example.com");
        assertThat(saved.getTiNumber()).isNotEqualTo("DRAFT");
        assertThat(service.list("", 0).getContent()).extracting(EmployeeTaxInvoiceListItem::id).containsExactly(id);
        assertThat(service.list("100%_", 0).getTotalElements()).isEqualTo(1);
        assertThat(service.list("missing", 0).getContent()).isEmpty();
        assertThatThrownBy(() -> service.getInvoice(-1L)).isInstanceOf(TaxInvoiceNotFoundException.class);
    }

    @Test
    void duplicateSubmissionReturnsOriginalInvoiceWithoutOverwritingBillingDetails() {
        String token = UUID.randomUUID().toString();
        long id = generate(token);
        details.setBilledTo("Changed recipient");
        assertThat(generate(token)).isEqualTo(id);
        assertThat(service.getInvoice(id).getBilledTo()).isEqualTo("Department");
        assertThat(service.list("", 0).getTotalElements()).isEqualTo(1);
        verify(numbers, times(1)).generate(any());
    }

    @Test
    void concurrentSubmissionsSaveExactlyOneInvoice() throws Exception {
        CyclicBarrier bothReady = new CyclicBarrier(2);
        when(numbers.generate(any())).thenAnswer(call -> {
            String number = "TI-2026-27-" + sequence.incrementAndGet();
            bothReady.await(10, TimeUnit.SECONDS);
            return number;
        });
        String token = UUID.randomUUID().toString();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> generate(token));
            var second = executor.submit(() -> generate(token));
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(second.get(15, TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("select count(*) from employee_tax_invoice", Long.class)).isEqualTo(1L);
    }

    @Test
    void failedSaveRollsBackAndSameDraftCanBeRetried() {
        String token = UUID.randomUUID().toString();
        details.setBilledTo("x".repeat(256));
        assertThatThrownBy(() -> generate(token)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(service.list("", 0).getTotalElements()).isZero();
        assertThat(preview.getBilledTo()).isEqualTo("Department");
        details.setBilledTo("Corrected recipient");
        assertThat(service.getInvoice(generate(token)).getBilledTo()).isEqualTo("Corrected recipient");
    }

    @Test
    void listsNewestInvoicesInBoundedPagesAndDoesNotTreatSearchWildcardsAsPatterns() {
        for (int i = 0; i < 21; i++) {
            generate(UUID.randomUUID().toString());
        }
        assertThat(service.list("", 0).getNumberOfElements()).isEqualTo(20);
        assertThat(service.list("", 1).getNumberOfElements()).isEqualTo(1);
        assertThat(service.list("100%_", 0).getTotalElements()).isEqualTo(21);
        assertThat(service.list("100%Z", 0).getTotalElements()).isZero();
        assertThat(service.list("", -1).getNumber()).isZero();
    }

    @Test
    void rejectsUnbillablePreviewWithoutWritingAnInvoice() {
        preview.setLineItems(List.of());
        assertThatThrownBy(() -> generate(UUID.randomUUID().toString())).isInstanceOf(TaxInvoiceException.class);
        assertThat(service.list("", 0).getTotalElements()).isZero();
        verifyNoInteractions(numbers);
    }
}

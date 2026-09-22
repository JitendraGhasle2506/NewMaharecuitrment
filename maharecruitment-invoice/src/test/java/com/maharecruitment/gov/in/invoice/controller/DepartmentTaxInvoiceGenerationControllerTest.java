package com.maharecruitment.gov.in.invoice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.maharecruitment.gov.in.invoice.repository.EmployeeTaxInvoiceRepository;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceExceptionHandler;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.maharecruitment.gov.in.invoice.dto.*;
import com.maharecruitment.gov.in.invoice.service.*;

class DepartmentTaxInvoiceGenerationControllerTest {
    private static final String URL = "/invoice/tax-invoices/generate";
    private final DepartmentTaxInvoiceGenerationService service = mock(DepartmentTaxInvoiceGenerationService.class);
    private final DepartmentTaxInvoiceService referenceService = mock(DepartmentTaxInvoiceService.class);
    private MockMvc mvc;
    private MockHttpSession session;
    private TaxInvoiceView invoice;
    private String savedSnapshot;
    private EmployeeTaxInvoiceService employeeInvoices;
    private final EmployeeTaxInvoiceRepository savedInvoices = mock(EmployeeTaxInvoiceRepository.class);

    @BeforeEach
    void setup() {
        ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");
        templates.setSuffix(".html");
        templates.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templates);
        ThymeleafViewResolver views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        TaxInvoiceNumberGenerator numbers = mock(TaxInvoiceNumberGenerator.class);
        when(numbers.generate(any())).thenReturn("TI-2026-27-00001");
        employeeInvoices = new EmployeeTaxInvoiceService(savedInvoices, numbers,
                JsonMapper.builder().findAndAddModules().build());
        when(savedInvoices.save(any(), any(), any(), any())).thenAnswer(call -> {
            savedSnapshot = call.getArgument(3);
            return 42L;
        });
        when(savedInvoices.findSnapshotById(42L)).thenAnswer(call -> Optional.ofNullable(savedSnapshot));
        mvc = MockMvcBuilders.standaloneSetup(new DepartmentTaxInvoiceGenerationController(service,
                new TaxInvoiceQrCodeGenerator(), employeeInvoices), new TaxInvoiceController(referenceService, new TaxInvoiceQrCodeGenerator()))
                .setControllerAdvice(new TaxInvoiceExceptionHandler())
                .setViewResolvers(views).build();
        session = new MockHttpSession();
        invoice = TaxInvoiceView.builder().tiNumber("EMP-1").tiDate(LocalDate.of(2026, 9, 21))
                .deptRefDate(LocalDate.of(2026, 9, 1)).requestId("OLD").deptRefNumber("OLD")
                .clientGstNumber("27AAKCM6988L1ZG").clientGstinAvailable(true).placeOfSupply("Maharashtra")
                .billedTo("Original department").billingAddress("Original address")
                .baseAmount(new BigDecimal("1000")).totalAmount(new BigDecimal("1180"))
                .totalAmountDisplay("1,180.00").companyName("MahaIT").documentTitle("TAX INVOICE")
                .panNumber("ABCDE1545T").gstNumber("27ABCDE1545TK1Z7").build();
        invoice.setProjectName("Test Project");
        invoice.setLineItems(List.of(TaxInvoiceLineItemView.builder().description("Alex - Developer")
                .lineNumber(1).quantity(1).totalAmount(new BigDecimal("1000")).build()));
        when(service.buildEmployeeInvoice(any())).thenReturn(invoice);
        when(service.loadProjectEmployees(any())).thenReturn(List.of(new TaxInvoiceEmployeePreviewView(
                1L, "E1", "Alex", "", "Developer", "L1", "", "", "", "Project", null, null, 30)));
    }

    private MockHttpServletRequestBuilder selection(String endpoint) {
        return post(URL + endpoint).session(session).param("departmentId", "1").param("projectId", "2")
                .param("startDate", "2026-09-01").param("endDate", "2026-09-30");
    }

    private String load() throws Exception {
        MvcResult result = mvc.perform(selection("/load")).andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("Preview Tax Invoice")
                .doesNotContain("generate/invoice\"");
        return (String) result.getModelAndView().getModel().get("loadToken");
    }

    private String preview() throws Exception {
        String token = load();
        MvcResult result = mvc.perform(selection("/employee-preview").param("loadToken", token))
                .andExpect(view().name("invoice/employee-tax-invoice-preview")).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("Generate Tax Invoice", "Original department",
                "Original address", "27AAKCM6988L1ZG", "billingDetailsForm", "Bank Account Details")
                .contains("employee-billing-preview", ">TAX INVOICE</h3>",
                        "MAHARASHTRA INFORMATION TECHNOLOGY CORPORATION LIMITED", "Shared navigation",
                        "alt=\"Tax Invoice QR Code\"", "data:image/png;base64,", "ABCDE1545T", "27ABCDE1545TK1Z7")
                .doesNotContain("id=\"printBtn\"", "PROFORMA INVOICE", ">MahaIT</h3>");
        return token;
    }

    private MockHttpServletRequestBuilder generate(String token) {
        return post(URL + "/invoice").session(session).param("loadToken", token)
                .param("clientGstNumber", "").param("placeOfSupply", "Pune")
                .param("requestId", "REQ-NEW").param("workOrderDate", "2026-08-15")
                .param("billedTo", "Entered recipient").param("billingAddress", "First line\nSecond line");
    }

    @Test
    void generatesEnteredDetailsAndQrFromPreviewSnapshotWithOptionalGst() throws Exception {
        String token = preview();
        mvc.perform(generate(token).param("totalAmount", "1").param("projectId", "999").principal(() -> "hr@example.com"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(URL + "/invoices/42"))
                .andExpect(flash().attributeExists("successMessage"));
        MvcResult result = mvc.perform(get(URL + "/invoices/42")).andExpect(status().isOk()).andReturn();
        TaxInvoiceView finalInvoice = (TaxInvoiceView) result.getModelAndView().getModel().get("invoice");
        assertThat(finalInvoice.getBilledTo()).isEqualTo("Entered recipient");
        assertThat(finalInvoice.getBillingAddress()).isEqualTo("First line\nSecond line");
        assertThat(finalInvoice.getDeptRefNumber()).isEqualTo("REQ-NEW");
        assertThat(finalInvoice.getDeptRefDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(finalInvoice.getPlaceOfSupply()).isEqualTo("Pune");
        assertThat(finalInvoice.isClientGstinAvailable()).isFalse();
        assertThat(finalInvoice.getClientGstNumber()).isNull();
        assertThat(finalInvoice.getTotalAmount()).isEqualByComparingTo("1180");
        assertThat(finalInvoice.getTiNumber()).isEqualTo("TI-2026-27-00001");
        assertThat(finalInvoice.getGeneratedByLoginId()).isEqualTo("hr@example.com");
        assertThat(invoice.getBilledTo()).isEqualTo("Original department");
        verify(savedInvoices).save(eq(token), argThat(filter -> filter.getProjectId().equals(2L)), any(), any());
        byte[] png = Base64.getDecoder().decode(finalInvoice.getQrCodeDataUrl().split(",", 2)[1]);
        String qr = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(png)))))).getText();
        assertThat(qr).contains("MAHAIT TAX INVOICE", "REQUEST ID: REQ-NEW", "BILLED TO: Entered recipient", "TOTAL: INR 1180.00");
        assertThat(result.getResponse().getContentAsString()).contains("id=\"printBtn\"", "REQ-NEW")
                .contains(">TAX INVOICE</h3>", "MAHARASHTRA INFORMATION TECHNOLOGY CORPORATION LIMITED")
                .doesNotContain("PROFORMA INVOICE")
                .doesNotContain("Generate Tax Invoice", "billingDetailsForm");
        verify(service, times(1)).buildEmployeeInvoice(any());
    }

    @ParameterizedTest
    @CsvSource({"clientGstNumber,invalid", "workOrderDate,not-a-date", "workOrderDate,2026-02-30", "billedTo,' '", "requestId,' '", "billingAddress,' '", "placeOfSupply,' '"})
    void validationRetainsEnteredFieldsAndPreview(String field, String invalid) throws Exception {
        String token = preview();
        MockHttpServletRequestBuilder request = generate(token);
        request.with(req -> { req.setParameter(field, invalid); return req; });
        MvcResult result = mvc.perform(request).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("billingDetails", field))
                .andExpect(model().attribute("employeeBillingPreview", true)).andReturn();
        TaxInvoiceBillingDetails retained = (TaxInvoiceBillingDetails) result.getModelAndView().getModel().get("billingDetails");
        if (!field.equals("billingAddress")) assertThat(retained.getBillingAddress()).isEqualTo("First line\nSecond line");
        assertThat(result.getResponse().getContentAsString()).contains("Generate Tax Invoice", "role=\"alert\"", "Shared navigation", "alt=\"Tax Invoice QR Code\"");
        verify(savedInvoices, never()).save(any(), any(), any(), any());
        mvc.perform(generate(token)).andExpect(redirectedUrl(URL + "/invoices/42"));
    }

    @ParameterizedTest
    @CsvSource({"departmentId,3", "subDepartmentId,4", "projectId,5", "startDate,2026-09-02", "endDate,2026-09-29"})
    void changedSelectionRequiresLoadAgain(String field, String changed) throws Exception {
        String token = load();
        mvc.perform(selection("/employee-preview").param("loadToken", token)
                .with(req -> { req.setParameter(field, changed); return req; }))
                .andExpect(view().name("invoice/tax-invoice-generation"))
                .andExpect(model().attributeExists("errorMessage"));
        verify(service, never()).buildEmployeeInvoice(any());
    }

    @Test
    void bypassAndStaleTokensCannotGenerate() throws Exception {
        mvc.perform(generate("missing")).andExpect(model().attributeExists("errorMessage"));
        String loaded = load();
        mvc.perform(generate(loaded)).andExpect(model().attributeExists("errorMessage"));
        String oldPreview = preview();
        load();
        mvc.perform(generate(oldPreview)).andExpect(model().attributeExists("errorMessage"));
        verify(savedInvoices, never()).save(any(), any(), any(), any());
    }

    @Test
    void suppliedGstIsPrintedAndLengthsAreValidated() throws Exception {
        String token = preview();
        mvc.perform(generate(token).with(req -> { req.setParameter("billedTo", "x".repeat(256)); return req; }))
                .andExpect(model().attributeHasFieldErrors("billingDetails", "billedTo"));
        mvc.perform(generate(token).with(req -> { req.setParameter("clientGstNumber", "27AAKCM6988L1ZG"); return req; }))
                .andExpect(redirectedUrl(URL + "/invoices/42"));
        assertThat(employeeInvoices.getInvoice(42L).isClientGstinAvailable()).isTrue();
        assertThat(employeeInvoices.getInvoice(42L).getClientGstNumber()).isEqualTo("27AAKCM6988L1ZG");
    }
    @Test
    void unregisteredDepartmentCanEnterRequiredBillingDetailsAndGenerate() throws Exception {
        invoice.setDepartmentRegistrationId(null);
        invoice.setBilledTo("");
        invoice.setBillingAddress("");
        invoice.setClientGstNumber(null);
        invoice.setClientGstinAvailable(false);
        String token = load();
        mvc.perform(selection("/employee-preview").param("loadToken", token))
                .andExpect(view().name("invoice/employee-tax-invoice-preview"))
                .andExpect(model().attribute("employeeBillingPreview", true));
        mvc.perform(generate(token).with(req -> { req.setParameter("billingAddress", ""); return req; }))
                .andExpect(model().attributeHasFieldErrors("billingDetails", "billingAddress"));
        mvc.perform(generate(token)).andExpect(redirectedUrl(URL + "/invoices/42"));
        TaxInvoiceView saved = employeeInvoices.getInvoice(42L);
        assertThat(saved.getDepartmentRegistrationId()).isNull();
        assertThat(saved.getBilledTo()).isEqualTo("Entered recipient");
        assertThat(saved.getBillingAddress()).isEqualTo("First line\nSecond line");
        assertThat(saved.isClientGstinAvailable()).isFalse();
    }

    @Test
    void repeatSubmissionRedirectsToSameInvoiceAndSavedInvoiceSurvivesSessionExpiry() throws Exception {
        String token = preview();
        mvc.perform(generate(token)).andExpect(redirectedUrl(URL + "/invoices/42"));
        mvc.perform(generate(token)).andExpect(redirectedUrl(URL + "/invoices/42"));
        verify(savedInvoices, times(1)).save(any(), any(), any(), any());
        session.invalidate();
        mvc.perform(get(URL + "/invoices/42")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("View Generated Invoices")));
    }

    @Test
    void failedSaveRetainsBillingDetailsAndAllowsRetry() throws Exception {
        String token = preview();
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("Database unavailable"))
                .doAnswer(call -> { savedSnapshot = call.getArgument(3); return 42L; })
                .when(savedInvoices).save(any(), any(), any(), any());
        MvcResult result = mvc.perform(generate(token)).andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(view().name("invoice/employee-tax-invoice-preview")).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("Unable to save", "Entered recipient", "First line");
        assertThat(invoice.getBilledTo()).isEqualTo("Original department");
        mvc.perform(generate(token)).andExpect(redirectedUrl(URL + "/invoices/42"));
    }

    @Test
    void generatedListRendersSavedInvoiceLinksAndSearchPagination() throws Exception {
        EmployeeTaxInvoiceListItem item = new EmployeeTaxInvoiceListItem(42L, "TI-2026-27-00001",
                LocalDate.now(), "REQ-NEW", "Test Project", "Entered recipient", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30), new BigDecimal("1180"), java.time.LocalDateTime.now());
        when(savedInvoices.findAll(eq("Test"), any())).thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 21));
        MvcResult result = mvc.perform(get(URL + "/invoices").param("search", " Test "))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("Generated Tax Invoices", "TI-2026-27-00001",
                URL + "/invoices/42", "1,180.00", "Next", "page=1", "search=Test");
        when(savedInvoices.findAll(eq(""), any())).thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get(URL + "/invoices")).andExpect(content().string(
                org.hamcrest.Matchers.containsString("No tax invoices have been generated yet.")));
    }

    @Test
    void existingApplicationInvoiceKeepsItsCompanyHeadingAndDocumentTitle() throws Exception {
        invoice.setDocumentTitle("PROFORMA INVOICE");
        when(referenceService.getInvoiceByApplicationId(38L)).thenReturn(invoice);
        MvcResult result = mvc.perform(get("/invoice/tax-invoices/application/38/new"))
                .andExpect(view().name("invoice/tax-invoice-preview")).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains(">MahaIT</h3>", "PROFORMA INVOICE")
                .doesNotContain("billingDetailsForm", "Generate Tax Invoice", "Shared navigation");
    }

}

package com.maharecruitment.gov.in.invoice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
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
    private MockMvc mvc;
    private MockHttpSession session;
    private TaxInvoiceView invoice;

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
        mvc = MockMvcBuilders.standaloneSetup(new DepartmentTaxInvoiceGenerationController(service,
                new TaxInvoiceQrCodeGenerator())).setViewResolvers(views).build();
        session = new MockHttpSession();
        invoice = TaxInvoiceView.builder().tiNumber("EMP-1").tiDate(LocalDate.of(2026, 9, 21))
                .deptRefDate(LocalDate.of(2026, 9, 1)).requestId("OLD").deptRefNumber("OLD")
                .clientGstNumber("27AAKCM6988L1ZG").clientGstinAvailable(true).placeOfSupply("Maharashtra")
                .billedTo("Original department").billingAddress("Original address")
                .baseAmount(new BigDecimal("1000")).totalAmount(new BigDecimal("1180"))
                .totalAmountDisplay("1,180.00").companyName("MahaIT").build();
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
                .andExpect(view().name("invoice/tax-invoice-preview")).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("Generate Tax Invoice", "Original department",
                "Original address", "27AAKCM6988L1ZG", "billingDetailsForm", "Bank Account Details")
                .doesNotContain("id=\"printBtn\"");
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
        MvcResult result = mvc.perform(generate(token).param("totalAmount", "1").param("projectId", "999"))
                .andExpect(status().isOk()).andReturn();
        TaxInvoiceView finalInvoice = (TaxInvoiceView) result.getModelAndView().getModel().get("invoice");
        assertThat(finalInvoice.getBilledTo()).isEqualTo("Entered recipient");
        assertThat(finalInvoice.getBillingAddress()).isEqualTo("First line\nSecond line");
        assertThat(finalInvoice.getDeptRefNumber()).isEqualTo("REQ-NEW");
        assertThat(finalInvoice.getDeptRefDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(finalInvoice.getPlaceOfSupply()).isEqualTo("Pune");
        assertThat(finalInvoice.isClientGstinAvailable()).isFalse();
        assertThat(finalInvoice.getClientGstNumber()).isNull();
        assertThat(finalInvoice.getTotalAmount()).isEqualByComparingTo("1180");
        byte[] png = Base64.getDecoder().decode(finalInvoice.getQrCodeDataUrl().split(",", 2)[1]);
        String qr = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(png)))))).getText();
        assertThat(qr).contains("REQUEST ID: REQ-NEW", "BILLED TO: Entered recipient", "TOTAL: INR 1180.00");
        assertThat(result.getResponse().getContentAsString()).contains("id=\"printBtn\"", "REQ-NEW")
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
        assertThat(result.getResponse().getContentAsString()).contains("Generate Tax Invoice", "role=\"alert\"");
        mvc.perform(generate(token)).andExpect(model().attributeDoesNotExist("employeeBillingPreview"));
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
    }

    @Test
    void suppliedGstIsPrintedAndLengthsAreValidated() throws Exception {
        String token = preview();
        mvc.perform(generate(token).with(req -> { req.setParameter("billedTo", "x".repeat(256)); return req; }))
                .andExpect(model().attributeHasFieldErrors("billingDetails", "billedTo"));
        mvc.perform(generate(token).with(req -> { req.setParameter("clientGstNumber", "27AAKCM6988L1ZG"); return req; }))
                .andExpect(status().isOk());
        assertThat(invoice.isClientGstinAvailable()).isTrue();
        assertThat(invoice.getClientGstNumber()).isEqualTo("27AAKCM6988L1ZG");
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
                .andExpect(view().name("invoice/tax-invoice-preview"))
                .andExpect(model().attribute("employeeBillingPreview", true));
        mvc.perform(generate(token).with(req -> { req.setParameter("billingAddress", ""); return req; }))
                .andExpect(model().attributeHasFieldErrors("billingDetails", "billingAddress"));
        mvc.perform(generate(token)).andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("employeeBillingPreview"));
        assertThat(invoice.getDepartmentRegistrationId()).isNull();
        assertThat(invoice.getBilledTo()).isEqualTo("Entered recipient");
        assertThat(invoice.getBillingAddress()).isEqualTo("First line\nSecond line");
        assertThat(invoice.isClientGstinAvailable()).isFalse();
        assertThat(invoice.getQrCodeDataUrl()).startsWith("data:image/png;base64,");
    }

}
